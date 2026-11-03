package com.nexgen.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Consumer-side view of {@code product-created} / {@code product-updated} / {@code product-deleted}. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductEvent {

    private String eventId;

    private String skuCode;

    /** CREATED, STOCK_UPDATED or DELETED. */
    private String eventType;

    private String name;

    private int quantity;

    private long timestamp;
}
