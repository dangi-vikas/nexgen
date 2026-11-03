package com.nexgen.cart_service.dto;

public class RemoveFromCartEvent {
    private String userId;
    private String productId;
    private int quantity;
    private String message;

    public RemoveFromCartEvent() {
    }

    public RemoveFromCartEvent(String userId, String productId, int quantity, String message) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}