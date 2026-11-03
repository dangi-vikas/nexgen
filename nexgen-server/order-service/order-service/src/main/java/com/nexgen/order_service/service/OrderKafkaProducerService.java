package com.nexgen.order_service.service;

import com.nexgen.order_service.dto.OrderCreatedEvent;
import com.nexgen.order_service.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Enqueues order domain events into the transactional outbox.
 *
 * <p>Despite the historical name this class no longer touches Kafka: writing to
 * the outbox inside the caller's transaction guarantees the event is published if
 * and only if the order change commits, and {@link OutboxPublisher} does the actual
 * sending with retries. Event type and topic names are identical by design.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderKafkaProducerService {

    static final String ORDER_CREATED = "order-created";
    static final String ORDER_CONFIRMED = "order-confirmed";
    static final String ORDER_CANCELLED = "order-cancelled";
    static final String ORDER_STATUS_UPDATED = "order-status-updated";

    private final OutboxService outboxService;

    public String sendOrderCreatedEvent(OrderCreatedEvent event) {
        String eventId = event.getEventId() == null ? UUID.randomUUID().toString() : event.getEventId();
        event.setEventId(eventId);
        event.setEventType(ORDER_CREATED);
        log.info("Enqueuing Order Created event {} for order {}", eventId, event.getOrderNumber());
        return enqueue(eventId, event.getOrderNumber(), ORDER_CREATED, event.getCorrelationId(), event);
    }

    public String sendOrderConfirmedEvent(OrderEvent event) {
        String eventId = event.getEventId() == null ? UUID.randomUUID().toString() : event.getEventId();
        event.setEventId(eventId);
        event.setEventType(ORDER_CONFIRMED);
        log.info("Enqueuing Order Confirmed event {} for order {}", eventId, event.getOrderNumber());
        return enqueue(eventId, event.getOrderNumber(), ORDER_CONFIRMED, event.getCorrelationId(), event);
    }

    public String sendOrderCancelledEvent(OrderEvent event) {
        String eventId = event.getEventId() == null ? UUID.randomUUID().toString() : event.getEventId();
        event.setEventId(eventId);
        event.setEventType(ORDER_CANCELLED);
        log.info("Enqueuing Order Cancelled event {} for order {}", eventId, event.getOrderNumber());
        return enqueue(eventId, event.getOrderNumber(), ORDER_CANCELLED, event.getCorrelationId(), event);
    }

    public String sendOrderUpdatedEvent(OrderEvent event) {
        String eventId = event.getEventId() == null ? UUID.randomUUID().toString() : event.getEventId();
        event.setEventId(eventId);
        event.setEventType(ORDER_STATUS_UPDATED);
        log.info("Enqueuing Order Status Updated event {} for order {}", eventId, event.getOrderNumber());
        return enqueue(eventId, event.getOrderNumber(), ORDER_STATUS_UPDATED, event.getCorrelationId(), event);
    }

    private String enqueue(String eventId, String orderNumber, String eventType, String correlationId, Object payload) {
        return outboxService.enqueue(eventId, "ORDER", orderNumber, eventType, "1.0", "order-service",
                correlationId, null, payload);
    }
}
