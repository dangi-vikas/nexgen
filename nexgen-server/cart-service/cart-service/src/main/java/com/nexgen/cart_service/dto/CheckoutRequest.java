package com.nexgen.cart_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckoutRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Payment Method is required")
    private String paymentMethod; // e.g., "CARD", "UPI", "COD"
}
