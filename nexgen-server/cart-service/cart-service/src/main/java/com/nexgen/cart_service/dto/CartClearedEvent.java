package com.nexgen.cart_service.dto;

public class CartClearedEvent {
    private String userId;
    private int quantity;
    private String message;

    public CartClearedEvent() {
    }

    public CartClearedEvent(String userId, int quantity, String message) {
        this.userId = userId;
        this.quantity = quantity;
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
