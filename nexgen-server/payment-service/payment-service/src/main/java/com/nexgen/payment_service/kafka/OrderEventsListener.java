package com.nexgen.payment_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.payment_service.dto.OrderLifecycleEvent;
import com.nexgen.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Payment side of the checkout saga.
 *
 * <ul>
 *   <li>{@code order-confirmed} → charge the order total (amount comes from the
 *       order, never from the client) and publish the authorisation result</li>
 *   <li>{@code order-cancelled} → refund an authorised payment</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventsListener {

    private final ObjectMapper objectMapper;
    private final ProcessedEventGuard guard;
    private final PaymentService paymentService;

    @Value("${payments.currency:EUR}")
    private String defaultCurrency;

    @KafkaListener(topics = "${topic.order-confirmed}", groupId = "payment-service")
    public void onOrderConfirmed(ConsumerRecord<String, String> record) {
        OrderLifecycleEvent event = read(record);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate order-confirmed ignored");
            return;
        }
        withCorrelation(event.getCorrelationId(), () -> paymentService.authorize(
                event.getOrderNumber(),
                event.getUserId(),
                event.getTotalAmount(),
                event.getCurrency() == null ? defaultCurrency : event.getCurrency(),
                event.getCorrelationId()));
        guard.markProcessed(event.getEventId(), "order-confirmed");
    }

    @KafkaListener(topics = "${topic.order-cancelled}", groupId = "payment-service")
    public void onOrderCancelled(ConsumerRecord<String, String> record) {
        OrderLifecycleEvent event = read(record);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate order-cancelled ignored");
            return;
        }
        withCorrelation(event.getCorrelationId(), () ->
                paymentService.refund(event.getOrderNumber(), event.getCorrelationId()));
        guard.markProcessed(event.getEventId(), "order-cancelled");
    }

    private OrderLifecycleEvent read(ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            log.warn("Skipping empty record from {}", record.topic());
            return null;
        }
        try {
            return objectMapper.readValue(record.value(), OrderLifecycleEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unreadable order event from " + record.topic(), e);
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
