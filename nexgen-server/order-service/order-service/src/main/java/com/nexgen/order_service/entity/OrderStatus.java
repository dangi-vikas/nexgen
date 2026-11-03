package com.nexgen.order_service.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    CREATED,
    DELIVERED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            // CREATED means "awaiting the inventory reservation result", so it can be
            // confirmed or cancelled as soon as inventory answers.
            case CREATED -> newStatus == PENDING || newStatus == CONFIRMED || newStatus == CANCELLED;
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == DELIVERED || newStatus == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}