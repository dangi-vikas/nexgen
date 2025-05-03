package com.nexgen.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartClearedEvent {
    private String userId;
    private int quantity;
    private String message;
}
