package com.nexgen.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CheckoutResponse {

    private String userId;
    private String paymentStatus;
    private double totalAmount;
    private List<String> purchasedProductIds;

}
