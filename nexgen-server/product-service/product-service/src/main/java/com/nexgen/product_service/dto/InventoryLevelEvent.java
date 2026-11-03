package com.nexgen.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Consumer-side view of {@code inventory-events-topic}, published by inventory. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryLevelEvent {

    private String eventId;

    private String skuCode;

    /** CREATED, UPDATED or OUT_OF_STOCK. */
    private String action;

    private int quantity;

    private Instant eventTime;
}
