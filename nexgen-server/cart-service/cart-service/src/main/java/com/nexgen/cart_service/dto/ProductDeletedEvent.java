package com.nexgen.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Consumer-side view of {@code product-deleted}, published by the product service. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDeletedEvent {

    private String eventId;

    private String skuCode;

    private String eventType;

    private String name;

    private int quantity;

    private long timestamp;
}
