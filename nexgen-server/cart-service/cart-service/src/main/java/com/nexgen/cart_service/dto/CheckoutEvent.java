package com.nexgen.cart_service.dto;

import java.util.List;

public class CheckoutEvent {
    private String userId;
    private List<String> productIds;
    private double totalAmount;
    private String message;

    public CheckoutEvent() {
    }

    public CheckoutEvent(String userId, List<String> productIds, double totalAmount, String message) {
        this.userId = userId;
        this.productIds = productIds;
        this.totalAmount = totalAmount;
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
