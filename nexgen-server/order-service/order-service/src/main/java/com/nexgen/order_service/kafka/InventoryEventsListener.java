package com.nexgen.order_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.order_service.dto.InventoryRejectedEvent;
import com.nexgen.order_service.dto.InventoryReservedEvent;
import com.nexgen.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the inventory service's answer to {@code order-created}.
 *
 * <p>This closes the checkout loop: inventory reserves stock and publishes
 * {@code inventory-reserved}, or refuses and publishes {@code inventory-rejected};
 * the order is confirmed or cancelled accordingly and publishes the matching
 * {@code order-confirmed} / {@code order-cancelled} event for cart, inventory and
 * notification to consume.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryEventsListener {

    private final ObjectMapper objectMapper;
    private final ProcessedEventGuard guard;
    private final OrderService orderService;

    @KafkaListener(topics = "${topic.inventory-reserved}", groupId = "order-service")
    public void onInventoryReserved(ConsumerRecord<String, String> record) {
        InventoryReservedEvent event = read(record, InventoryReservedEvent.class);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate inventory-reserved ignored: {}", event == null ? null : event.getEventId());
            return;
        }
        withCorrelation(event.getCorrelationId(), () -> {
            log.info("Inventory reserved stock for order {}", event.getOrderNumber());
            orderService.confirmAfterInventoryReservation(event.getOrderNumber());
        });
        guard.markProcessed(event.getEventId(), "inventory-reserved");
    }

    @KafkaListener(topics = "${topic.inventory-rejected}", groupId = "order-service")
    public void onInventoryRejected(ConsumerRecord<String, String> record) {
        InventoryRejectedEvent event = read(record, InventoryRejectedEvent.class);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate inventory-rejected ignored: {}", event == null ? null : event.getEventId());
            return;
        }
        withCorrelation(event.getCorrelationId(), () -> {
            log.warn("Inventory rejected order {}: {}", event.getOrderNumber(), event.getReason());
            orderService.cancelAfterInventoryRejection(event.getOrderNumber(), event.getReason());
        });
        guard.markProcessed(event.getEventId(), "inventory-rejected");
    }

    private <T> T read(ConsumerRecord<String, String> record, Class<T> type) {
        if (record.value() == null) {
            log.warn("Skipping empty record from {}", record.topic());
            return null;
        }
        try {
            return objectMapper.readValue(record.value(), type);
        } catch (Exception e) {
            // Fail loudly so DefaultErrorHandler retries and finally dead-letters the record.
            throw new IllegalArgumentException("Unreadable " + type.getSimpleName() + " from " + record.topic(), e);
        }
    }

    private void withCorrelation(String correlationId, Runnable action) {
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        try {
            action.run();
        } finally {
            MDC.remove("correlationId");
        }
    }
}
