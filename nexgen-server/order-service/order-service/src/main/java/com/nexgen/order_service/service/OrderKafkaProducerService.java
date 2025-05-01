package com.nexgen.order_service.service;

import com.nexgen.order_service.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderKafkaProducerService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${topic.order-created}")
    private String orderCreatedTopic;

    @Value("${topic.order-cancelled}")
    private String orderCancelledTopic;

    @Value("${topic.order-status-updated}")
    private String orderStatusUpdatedTopic;

    public void sendOrderCreatedEvent(OrderEvent event) {
        log.info("Publishing Order Created event: {}", event);
        kafkaTemplate.send(orderCreatedTopic, event);
    }

    public void sendOrderCancelledEvent(OrderEvent event) {
        log.info("Publishing Order Cancelled event: {}", event);
        kafkaTemplate.send(orderCancelledTopic, event);
    }

    public void sendOrderUpdatedEvent(OrderEvent event) {
        log.info("Publishing Order Status Update event: {}", event);
        kafkaTemplate.send(orderStatusUpdatedTopic, event);
    }
}
