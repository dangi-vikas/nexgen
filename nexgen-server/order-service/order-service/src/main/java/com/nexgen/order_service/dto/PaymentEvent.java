package com.nexgen.order_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/** Consumer-side view of {@code payment-authorized} / {@code payment-failed} / {@code payment-refunded}. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
