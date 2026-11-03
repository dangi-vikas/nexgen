package com.nexgen.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload of {@code cart-checkout}: a checkout was accepted for the user and an
 * order is awaiting stock confirmation. Published from the same transaction that
 * writes it, so it is never sent for a checkout that failed to reach the order
 * service.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutEvent {

    private String eventId;

    private String eventType;

    private String userId;

    private String orderNumber;

    private List<String> productIds;

    private double totalAmount;

    private String message;
}
