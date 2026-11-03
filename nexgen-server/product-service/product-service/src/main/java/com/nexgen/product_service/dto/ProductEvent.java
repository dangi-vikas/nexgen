package com.nexgen.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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