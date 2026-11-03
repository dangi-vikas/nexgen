package com.nexgen.payment_service.service;

import java.math.BigDecimal;

/**
 * Port for the payment provider.
 *
 * <p>Swap {@link SimulatedPaymentGateway} for a Stripe/Adyen implementation without
 * touching the saga, the events or the database: a real adapter returns
 * {@code authorized=false} until its webhook confirms the payment, then the same
 * events are published.
 */
public interface PaymentGateway {

    String name();

    ChargeResult charge(String orderNumber, BigDecimal amount, String currency);

    RefundResult refund(String orderNumber, String providerRef, BigDecimal amount);

    record ChargeResult(boolean authorized, String providerRef, String failureReason) {
    }

    record RefundResult(boolean refunded, String failureReason) {
    }
}
