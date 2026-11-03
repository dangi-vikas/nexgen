package com.nexgen.order_service.saga;

import com.nexgen.order_service.dto.OrderCreatedEvent;
import com.nexgen.order_service.service.OrderKafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutSagaOrchestrator {
    private final OrderKafkaProducerService kafkaProducerService;

    public void start(OrderCreatedEvent event) {
        log.info("Starting checkout saga for order {} with correlationId {}", event.getOrderNumber(), event.getCorrelationId());
    }

    public void compensateInventory(String orderNumber) {
        log.info("Compensating inventory reservation for order {}", orderNumber);
    }

    public void compensatePayment(String orderNumber) {
        log.info("Compensating payment for order {}", orderNumber);
    }
}
