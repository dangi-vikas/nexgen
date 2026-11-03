package com.nexgen.order_service.dto;

import com.nexgen.order_service.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Canonical {@code order-created} payload. This is the only representation of
 * the event; it is written to the outbox and published by {@code OutboxPublisher}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {

    /** Stable id used by every consumer for idempotency. */
    private String eventId;

    private String eventType;

    private String orderNumber;

    private String userId;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private List<OrderItemEvent> items;

    private Instant timestamp;

    private String correlationId;

    private String idempotencyKey;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemEvent {
        private String skuCode;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal lineTotal;
    }
}
