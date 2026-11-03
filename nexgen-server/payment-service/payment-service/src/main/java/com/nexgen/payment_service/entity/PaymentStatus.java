package com.nexgen.payment_service.entity;

public enum PaymentStatus {
    /** Charge has been requested but not answered by the gateway. */
    PENDING,
    AUTHORIZED,
    FAILED,
    REFUNDED
}
