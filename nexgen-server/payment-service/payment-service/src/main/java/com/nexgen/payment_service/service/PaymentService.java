package com.nexgen.payment_service.service;

import com.nexgen.payment_service.dto.PaymentEvent;
import com.nexgen.payment_service.entity.Payment;
import com.nexgen.payment_service.entity.PaymentStatus;
import com.nexgen.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment saga participant.
 *
 * <p>{@code order-confirmed} triggers a charge; the outcome is written to the
 * payment row and announced as {@code payment-authorized} or {@code payment-failed}
 * in the same transaction. Cancelling a paid order refunds it and announces
 * {@code payment-refunded}.
 *
 * <p>Both operations are idempotent: a redelivered event finds the existing payment
 * row (or the REFUNDED status) and does nothing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    static final String PAYMENT_AUTHORIZED = "payment-authorized";
    static final String PAYMENT_FAILED = "payment-failed";
    static final String PAYMENT_REFUNDED = "payment-refunded";

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final OutboxService outboxService;

    @Transactional
    public void authorize(String orderNumber, String userId, BigDecimal amount, String currency,
                          String correlationId) {
        if (paymentRepository.findByOrderNumber(orderNumber).isPresent()) {
            log.info("Payment for order {} already exists, ignoring order-confirmed", orderNumber);
            return;
        }
        if (amount == null || amount.signum() <= 0) {
            log.error("Order {} has no payable amount ({}), refusing to charge", orderNumber, amount);
            return;
        }

        // Charge first, then write the row exactly once. Saving a PENDING row and updating
        // it afterwards made the second save re-insert, hitting uk_payments_order and
        // parking the event in an endless retry loop.
        PaymentGateway.ChargeResult result = paymentGateway.charge(orderNumber, amount, currency);

        Payment payment = Payment.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .amount(amount)
                .currency(currency)
                .status(result.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.FAILED)
                .provider(paymentGateway.name())
                .providerRef(result.authorized() ? result.providerRef() : null)
                .failureReason(result.authorized() ? null : result.failureReason())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
        paymentRepository.save(payment);

        publish(result.authorized() ? PAYMENT_AUTHORIZED : PAYMENT_FAILED, payment, correlationId);
    }

    @Transactional
    public void refund(String orderNumber, String correlationId) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber).orElse(null);
        if (payment == null) {
            log.info("No payment for cancelled order {}, nothing to refund", orderNumber);
            return;
        }
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            log.info("Payment for order {} is {}, nothing to refund", orderNumber, payment.getStatus());
            return;
        }

        PaymentGateway.RefundResult result =
                paymentGateway.refund(orderNumber, payment.getProviderRef(), payment.getAmount());
        if (!result.refunded()) {
            // Throwing keeps the event unprocessed so the consumer retries.
            throw new IllegalStateException("Refund failed for order " + orderNumber + ": " + result.failureReason());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);
        publish(PAYMENT_REFUNDED, payment, correlationId);
    }

    private void publish(String eventType, Payment payment, String correlationId) {
        String eventId = UUID.randomUUID().toString();
        PaymentEvent event = PaymentEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .orderNumber(payment.getOrderNumber())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .provider(payment.getProvider())
                .providerRef(payment.getProviderRef())
                .failureReason(payment.getFailureReason())
                .correlationId(correlationId)
                .occurredAt(Instant.now())
                .build();
        outboxService.enqueue(eventId, "PAYMENT", payment.getOrderNumber(), eventType, "1.0",
                "payment-service", correlationId, null, event);
        log.info("Payment {} for order {}: {}", eventType, payment.getOrderNumber(), payment.getStatus());
    }
}
