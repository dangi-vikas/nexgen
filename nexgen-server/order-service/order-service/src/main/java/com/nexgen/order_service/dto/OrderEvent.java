package com.nexgen.order_service.dto;

import com.nexgen.order_service.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lifecycle event published to {@code order-confirmed}, {@code order-cancelled}
 * and {@code order-status-updated}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEvent {

    /** Stable id used by every consumer for idempotency. */
    private String eventId;

    private String eventType;

    private String orderNumber;

    private String userId;

    private OrderStatus status;

    private Long timestamp;

    private String correlationId;

    /** Human readable explanation, mainly used by {@code order-cancelled}. */
    private String reason;
}
