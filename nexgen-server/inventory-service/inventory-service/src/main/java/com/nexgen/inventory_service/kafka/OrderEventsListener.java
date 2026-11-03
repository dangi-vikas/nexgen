package com.nexgen.inventory_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.inventory_service.dto.OrderCreatedEvent;
import com.nexgen.inventory_service.dto.OrderLifecycleEvent;
import com.nexgen.inventory_service.dto.ReservationItemRequest;
import com.nexgen.inventory_service.dto.ReservationRequest;
import com.nexgen.inventory_service.exception.InventoryReservationException;
import com.nexgen.inventory_service.service.InventoryOutcomePublisher;
import com.nexgen.inventory_service.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The inventory side of the checkout saga.
 *
 * <ul>
 *   <li>{@code order-created}     → reserve stock, publish {@code inventory-reserved}
 *                                   or {@code inventory-rejected}</li>
 *   <li>{@code order-confirmed}   → turn the reservation into a real stock deduction</li>
 *   <li>{@code order-cancelled}   → release the reservation</li>
 * </ul>
 *
 * Every handler is idempotent: reservations are keyed by order number, so a
 * redelivered event cannot reserve twice.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventsListener {

    private final ObjectMapper objectMapper;
    private final ProcessedEventGuard guard;
    private final InventoryReservationService reservationService;
    private final InventoryOutcomePublisher outcomePublisher;

    @KafkaListener(topics = "${topic.order-created}", groupId = "inventory-service")
    public void onOrderCreated(ConsumerRecord<String, String> record) {
        OrderCreatedEvent event = read(record, OrderCreatedEvent.class);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate order-created ignored");
            return;
        }

        withCorrelation(event.getCorrelationId(), () -> {
            ReservationRequest request = toReservationRequest(event);
            try {
                reservationService.reserve(request);
            } catch (InventoryReservationException | com.nexgen.inventory_service.exception.InsufficientStockException
                     | com.nexgen.inventory_service.exception.ItemNotFoundException e) {
                // The reservation transaction has rolled back; record the refusal so the
                // order service can cancel the order and compensate.
                log.warn("Cannot reserve stock for order {}: {}", event.getOrderNumber(), e.getMessage());
                outcomePublisher.rejected(event.getOrderNumber(), event.getCorrelationId(), e.getMessage());
            }
        });

        guard.markProcessed(event.getEventId(), "order-created");
    }

    @KafkaListener(topics = "${topic.order-confirmed}", groupId = "inventory-service")
    public void onOrderConfirmed(ConsumerRecord<String, String> record) {
        OrderLifecycleEvent event = read(record, OrderLifecycleEvent.class);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate order-confirmed ignored");
            return;
        }
        withCorrelation(event.getCorrelationId(), () ->
                reservationService.confirm(event.getOrderNumber()));
        guard.markProcessed(event.getEventId(), "order-confirmed");
    }

    @KafkaListener(topics = "${topic.order-cancelled}", groupId = "inventory-service")
    public void onOrderCancelled(ConsumerRecord<String, String> record) {
        OrderLifecycleEvent event = read(record, OrderLifecycleEvent.class);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate order-cancelled ignored");
            return;
        }
        withCorrelation(event.getCorrelationId(), () ->
                reservationService.release(event.getOrderNumber()));
        guard.markProcessed(event.getEventId(), "order-cancelled");
    }

    private ReservationRequest toReservationRequest(OrderCreatedEvent event) {
        List<ReservationItemRequest> items = event.getItems() == null ? List.of()
                : event.getItems().stream()
                .map(i -> ReservationItemRequest.builder()
                        .skuCode(i.getSkuCode())
                        .quantity(i.getQuantity())
                        .build())
                .toList();
        return ReservationRequest.builder()
                .orderNumber(event.getOrderNumber())
                .items(items)
                .correlationId(event.getCorrelationId())
                .build();
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
