package com.nexgen.order_service.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    CREATED,
    DELIVERED;

    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case CREATED -> newStatus == PENDING || newStatus == CANCELLED;
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == DELIVERED || newStatus == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}