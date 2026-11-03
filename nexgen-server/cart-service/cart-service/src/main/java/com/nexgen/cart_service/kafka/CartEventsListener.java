package com.nexgen.cart_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.cart_service.dto.OrderConfirmedEvent;
import com.nexgen.cart_service.dto.ProductDeletedEvent;
import com.nexgen.cart_service.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cart reactions to other services' events.
 *
 * <ul>
 *   <li>{@code order-confirmed} → clear the buyer's cart. This replaces deleting the
 *       cart inside the checkout call, so a rejected reservation leaves the cart
 *       untouched.</li>
 *   <li>{@code product-deleted} → drop that product from every cart.</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CartEventsListener {

    private final ObjectMapper objectMapper;
    private final ProcessedEventGuard guard;
    private final CartItemRepository cartRepository;

    @KafkaListener(topics = "${topic.order-confirmed}", groupId = "cart-service")
    @Transactional
    public void onOrderConfirmed(ConsumerRecord<String, String> record) {
        OrderConfirmedEvent event = read(record, OrderConfirmedEvent.class);
        if (event == null || guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate order-confirmed ignored");
            return;
        }
        withCorrelation(event.getCorrelationId(), () -> {
            if (event.getUserId() == null || event.getUserId().isBlank()) {
                log.warn("order-confirmed {} has no userId, cannot clear a cart", event.getOrderNumber());
                return;
            }
            log.info("Order {} confirmed, clearing cart of user {}", event.getOrderNumber(), event.getUserId());
            cartRepository.deleteByUserId(event.getUserId());
        });
        guard.markProcessed(event.getEventId(), "order-confirmed");
    }

    @KafkaListener(topics = "${topic.product-deleted}", groupId = "cart-service")
    @Transactional
    public void onProductDeleted(ConsumerRecord<String, String> record) {
        ProductDeletedEvent event = read(record, ProductDeletedEvent.class);
        if (event == null) {
            return;
        }
        String dedupeKey = event.getEventId() != null ? event.getEventId() : "product-deleted:" + event.getSkuCode();
        if (guard.isProcessed(dedupeKey)) {
            log.debug("Duplicate product-deleted ignored for {}", event.getSkuCode());
            return;
        }
        log.info("Product {} deleted, removing it from all carts", event.getSkuCode());
        cartRepository.deleteByProductId(event.getSkuCode());
        guard.markProcessed(dedupeKey, "product-deleted");
    }

    private <T> T read(ConsumerRecord<String, String> record, Class<T> type) {
        if (record.value() == null) {
            log.warn("Skipping empty record from {}", record.topic());
            return null;
        }
        try {
            return objectMapper.readValue(record.value(), type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unreadable " + type.getSimpleName() + " from " + record.topic(), e);
        }
    }

    private void withCorrelation(String correlationId, Runnable action) {
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        try {
            action.run();
        } finally {
            MDC.remove("correlationId");
        }
    }
}
