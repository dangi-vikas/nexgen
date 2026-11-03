package com.nexgen.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Stock level change published to {@code inventory-events-topic}. The product
 * service consumes it to keep its own quantity mirror in sync.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryEvent implements Serializable {
    private String eventId;
    private String skuCode;
    /** CREATED, UPDATED or OUT_OF_STOCK. */
    private String action;
    private int quantity;
    private Instant eventTime;
}
