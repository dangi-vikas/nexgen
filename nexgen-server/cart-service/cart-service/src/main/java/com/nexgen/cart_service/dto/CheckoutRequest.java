package com.nexgen.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutRequest {
    private String userId;
    private String paymentMethod; // e.g., "CARD", "UPI", "COD"
}
