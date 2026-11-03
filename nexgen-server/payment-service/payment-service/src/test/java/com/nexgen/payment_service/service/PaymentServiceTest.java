package com.nexgen.payment_service.service;

import com.nexgen.payment_service.entity.Payment;
import com.nexgen.payment_service.entity.PaymentStatus;
import com.nexgen.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxService outboxService;

    private SimulatedPaymentGateway gateway;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        gateway = new SimulatedPaymentGateway();
        paymentService = new PaymentService(paymentRepository, gateway, outboxService);
    }

    private Payment existing(PaymentStatus status, String providerRef) {
        return Payment.builder()
                .orderNumber("ORD-1")
                .userId("user1")
                .amount(BigDecimal.valueOf(42.50))
                .currency("EUR")
                .status(status)
                .provider("simulator")
                .providerRef(providerRef)
                .build();
    }

    @Test
    void authorisedChargePublishesPaymentAuthorized() {
        when(paymentRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.empty());

        paymentService.authorize("ORD-1", "user1", BigDecimal.valueOf(42.50), "EUR", "corr-1");

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertEquals(PaymentStatus.AUTHORIZED, saved.getValue().getStatus());
        verify(outboxService).enqueue(anyString(), eq("PAYMENT"), eq("ORD-1"), eq("payment-authorized"),
                eq("1.0"), eq("payment-service"), eq("corr-1"), any(), any());
    }

    @Test
    void declinedChargePublishesPaymentFailed() {
        when(paymentRepository.findByOrderNumber("ORD-2")).thenReturn(Optional.empty());

        paymentService.authorize("ORD-2", "user1", BigDecimal.valueOf(42.99), "EUR", null);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertEquals(PaymentStatus.FAILED, saved.getValue().getStatus());
        verify(outboxService).enqueue(anyString(), eq("PAYMENT"), eq("ORD-2"), eq("payment-failed"),
                eq("1.0"), eq("payment-service"), any(), any(), any());
    }

    @Test
    void redeliveredOrderConfirmedDoesNotChargeTwice() {
        when(paymentRepository.findByOrderNumber("ORD-1"))
                .thenReturn(Optional.of(existing(PaymentStatus.AUTHORIZED, "sim_1")));

        paymentService.authorize("ORD-1", "user1", BigDecimal.valueOf(42.50), "EUR", null);

        verify(paymentRepository, never()).save(any());
        verify(outboxService, never()).enqueue(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancellingAPaidOrderRefunds() {
        when(paymentRepository.findByOrderNumber("ORD-1"))
                .thenReturn(Optional.of(existing(PaymentStatus.AUTHORIZED, "sim_1")));

        paymentService.refund("ORD-1", "corr-2");

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertEquals(PaymentStatus.REFUNDED, saved.getValue().getStatus());
        verify(outboxService).enqueue(anyString(), eq("PAYMENT"), eq("ORD-1"), eq("payment-refunded"),
                eq("1.0"), eq("payment-service"), eq("corr-2"), any(), any());
    }

    @Test
    void cancellingAnUnpaidOrderDoesNotRefund() {
        when(paymentRepository.findByOrderNumber("ORD-1"))
                .thenReturn(Optional.of(existing(PaymentStatus.FAILED, null)));

        paymentService.refund("ORD-1", null);

        verify(paymentRepository, never()).save(any());
        verify(outboxService, never()).enqueue(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
