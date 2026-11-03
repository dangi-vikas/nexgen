package com.nexgen.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Consumer-side view of {@code order-created}. Inventory no longer receives the
 * order over HTTP: the event carries the items, so the reservation is driven by
 * the same message every other service sees.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {

    private String eventId;

    private String eventType;

    private String orderNumber;

    private String userId;

    private String status;

    private BigDecimal totalAmount;

    private List<Item> items;

    private Instant timestamp;

    private String correlationId;

    private String idempotencyKey;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private String skuCode;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal lineTotal;
    }
}
