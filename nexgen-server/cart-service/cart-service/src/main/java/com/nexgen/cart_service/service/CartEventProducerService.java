package com.nexgen.cart_service.service;

import com.nexgen.cart_service.dto.CheckoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Enqueues cart events into the outbox.
 *
 * <p>Only {@code cart-checkout} is still published: the item-added / item-removed /
 * cart-cleared topics had no consumer anywhere, so they were removed rather than
 * left as noise. Everything else the cart does is a reaction to
 * {@code order-confirmed} and {@code product-deleted}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CartEventProducerService {

    static final String CART_CHECKOUT = "cart-checkout";

    private final OutboxService outboxService;

    public String sendCheckoutEvent(CheckoutEvent event) {
        String eventId = event.getEventId() == null ? UUID.randomUUID().toString() : event.getEventId();
        event.setEventId(eventId);
        event.setEventType(CART_CHECKOUT);
        log.info("Enqueuing Checkout event {} for user {}", eventId, event.getUserId());
        return outboxService.enqueue(eventId, "CART", event.getUserId(), CART_CHECKOUT, "1.0",
                "cart-service", null, null, event);
    }
}
