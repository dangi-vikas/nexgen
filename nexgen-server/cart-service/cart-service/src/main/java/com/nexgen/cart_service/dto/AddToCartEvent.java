package com.nexgen.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartEvent {
    private String userId;
    private String productId;
    private int quantity;
    private double price;
}
