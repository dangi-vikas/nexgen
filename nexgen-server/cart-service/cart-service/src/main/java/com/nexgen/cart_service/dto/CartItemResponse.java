package com.nexgen.cart_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemResponse {
    private String userId;
    private String productId;
    private int quantity;
    private double price;
}
