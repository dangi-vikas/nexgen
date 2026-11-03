package com.nexgen.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/** Payload of {@code inventory-reserved}, consumed by the order service. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservedEvent {

    private String eventId;

    private String eventType;

    private String orderNumber;

    private String correlationId;

    private Instant occurredAt;

    private List<Item> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private String skuCode;
        private Integer quantity;
    }
}
