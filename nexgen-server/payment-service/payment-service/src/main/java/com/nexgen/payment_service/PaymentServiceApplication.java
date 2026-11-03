package com.nexgen.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment service: consumes {@code order-confirmed}, charges through a
 * {@link com.nexgen.payment_service.service.PaymentGateway}, and publishes the
 * authorisation result back onto Kafka.
 *
 * <p>The default gateway is a simulator, so the whole saga runs with no provider
 * account and no money moving. A real PSP (for example Stripe in test mode, which
 * is free) only needs another implementation of the same interface.
 */
@SpringBootApplication
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
