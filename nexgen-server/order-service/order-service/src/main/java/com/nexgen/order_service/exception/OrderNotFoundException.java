package com.nexgen.order_service.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderNumber) {
        super("Order not found with order number: " + orderNumber);
    }
}
