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
    private String skuCode;
    private String eventType; // e.g., CREATED, UPDATED, DELETED
    private String name;
    private int quantity;
    private long timestamp;
}