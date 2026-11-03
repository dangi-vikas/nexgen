package com.nexgen.payment_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deterministic, offline gateway used when no provider is configured.
 *
 * <p>It never contacts a network and never moves money, which makes the saga
 * testable for free. The decline rule is deliberate and stable so failure and
 * compensation paths can be exercised: an amount whose cents are {@code 99} (for
 * example {@code 42.99}) is declined, everything else is authorised.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payments.gateway", havingValue = "simulator", matchIfMissing = true)
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public String name() {
        return "simulator";
    }

    @Override
    public ChargeResult charge(String orderNumber, BigDecimal amount, String currency) {
        if (amount == null || amount.signum() <= 0) {
            return new ChargeResult(false, null, "Amount must be positive");
        }
        if (amount.movePointRight(2).remainder(BigDecimal.valueOf(100)).intValue() == 99) {
            log.info("Simulated decline for order {} (amount {})", orderNumber, amount);
            return new ChargeResult(false, null, "Simulated decline: card declined by issuer");
        }
        String reference = "sim_" + UUID.randomUUID();
        log.info("Simulated authorisation {} for order {} ({} {})", reference, orderNumber, amount, currency);
        return new ChargeResult(true, reference, null);
    }

    @Override
    public RefundResult refund(String orderNumber, String providerRef, BigDecimal amount) {
        log.info("Simulated refund of {} for order {} (provider ref {})", amount, orderNumber, providerRef);
        return new RefundResult(true, null);
    }
}
