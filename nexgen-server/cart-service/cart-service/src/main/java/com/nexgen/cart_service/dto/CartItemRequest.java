package com.nexgen.cart_service.dto;

import lombok.Data;

@Data
public class CartItemRequest {
    private String productId;
    private int quantity;
    private double price;
}
