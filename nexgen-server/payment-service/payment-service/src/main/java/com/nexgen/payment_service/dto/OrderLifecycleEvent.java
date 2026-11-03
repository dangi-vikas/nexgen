package com.nexgen.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Consumer-side view of {@code order-confirmed} / {@code order-cancelled}. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderLifecycleEvent {

    private String eventId;

    private String eventType;

    private String orderNumber;

    private String userId;

    private String status;

    private BigDecimal totalAmount;

    private String currency;

    private Long timestamp;

    private String correlationId;

    private String reason;
}
