package com.nexgen.order_service.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAID,
    CANCELLED,
    CREATED,
    DELIVERED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            // CREATED means "awaiting the inventory reservation result", so it can be
            // confirmed or cancelled as soon as inventory answers.
            case CREATED -> newStatus == PENDING || newStatus == CONFIRMED || newStatus == CANCELLED;
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
            // CONFIRMED waits for the payment authorisation; PAID waits for fulfilment.
            case CONFIRMED -> newStatus == PAID || newStatus == DELIVERED || newStatus == CANCELLED;
            case PAID -> newStatus == DELIVERED || newStatus == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}