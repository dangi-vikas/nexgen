package com.nexgen.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutEvent {
    private String userId;
    private List<String> productIds;
    private double totalAmount;
    private String message;
}
