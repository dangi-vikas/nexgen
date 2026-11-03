package com.nexgen.order_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.order_service.dto.PaymentEvent;
import com.nexgen.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes the payment service's answer, closing the second half of the saga.
 *
 * <ul>
 *   <li>{@code payment-authorized} → CONFIRMED becomes PAID</li>
 *   <li>{@code payment-failed} → the order is cancelled, which makes inventory
 *       release the reservation</li>
 *   <li>{@code payment-refunded} → recorded for audit; no status change</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventsListener {

    private final ObjectMapper objectMapper;
    private final ProcessedEventGuard guard;
    private final OrderService orderService;

    @KafkaListener(topics = "${topic.payment-authorized}", groupId = "order-service")
    public void onPaymentAuthorized(ConsumerRecord<String, String> record) {
        PaymentEvent event = read(record);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate payment-authorized ignored");
            return;
        }
        withCorrelation(event.getCorrelationId(), () -> {
            log.info("Payment authorised for order {} ({})", event.getOrderNumber(), event.getProviderRef());
            orderService.markPaid(event.getOrderNumber());
        });
        guard.markProcessed(event.getEventId(), "payment-authorized");
    }

    @KafkaListener(topics = "${topic.payment-failed}", groupId = "order-service")
    public void onPaymentFailed(ConsumerRecord<String, String> record) {
        PaymentEvent event = read(record);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate payment-failed ignored");
            return;
        }
        withCorrelation(event.getCorrelationId(), () -> {
            log.warn("Payment failed for order {}: {}", event.getOrderNumber(), event.getFailureReason());
            orderService.cancelAfterPaymentFailure(event.getOrderNumber(),
                    event.getFailureReason() == null ? "Payment failed" : event.getFailureReason());
        });
        guard.markProcessed(event.getEventId(), "payment-failed");
    }

    @KafkaListener(topics = "${topic.payment-refunded}", groupId = "order-service")
    public void onPaymentRefunded(ConsumerRecord<String, String> record) {
        PaymentEvent event = read(record);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate payment-refunded ignored");
            return;
        }
        withCorrelation(event.getCorrelationId(), () ->
                orderService.recordRefund(event.getOrderNumber(), "Refunded " + event.getAmount() + " " + event.getCurrency()));
        guard.markProcessed(event.getEventId(), "payment-refunded");
    }

    private PaymentEvent read(ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            log.warn("Skipping empty record from {}", record.topic());
            return null;
        }
        try {
            return objectMapper.readValue(record.value(), PaymentEvent.class);
        } catch (Exception e) {
            // Fail loudly so DefaultErrorHandler retries and finally dead-letters it.
            throw new IllegalArgumentException("Unreadable payment event from " + record.topic(), e);
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
