package com.nexgen.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Consumer-side view of {@code order-confirmed}, published by the order service. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderConfirmedEvent {

    private String eventId;

    private String eventType;

    private String orderNumber;

    private String userId;

    private String status;

    private Long timestamp;

    private String correlationId;

    private String reason;

    private Instant occurredAt;
}
