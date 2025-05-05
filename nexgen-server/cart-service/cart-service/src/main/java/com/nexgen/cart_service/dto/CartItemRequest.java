package com.nexgen.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartItemRequest {
    private String productId;
    private int quantity;
    private double price;
}
