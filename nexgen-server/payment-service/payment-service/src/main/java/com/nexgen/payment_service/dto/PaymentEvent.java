package com.nexgen.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload of {@code payment-authorized}, {@code payment-failed} and
 * {@code payment-refunded}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {

    private String eventId;

    private String eventType;

    private String orderNumber;

    private String userId;

    private BigDecimal amount;

    private String currency;

    private String provider;

    private String providerRef;

    private String failureReason;

    private String correlationId;

    private Instant occurredAt;
}
