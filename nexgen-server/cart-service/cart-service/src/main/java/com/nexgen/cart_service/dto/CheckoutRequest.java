package com.nexgen.cart_service.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String userId;
    private String paymentMethod; // e.g., "CARD", "UPI", "COD"
}
