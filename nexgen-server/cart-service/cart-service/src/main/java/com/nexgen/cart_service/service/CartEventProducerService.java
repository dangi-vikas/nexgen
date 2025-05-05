package com.nexgen.cart_service.service;

import com.nexgen.cart_service.dto.AddToCartEvent;
import com.nexgen.cart_service.dto.CartClearedEvent;
import com.nexgen.cart_service.dto.CheckoutEvent;
import com.nexgen.cart_service.dto.RemoveFromCartEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartEventProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaFallbackService fallbackService;
    private static final String FALLBACK_ERROR = "Kafka send failed. Falling back to Redis: {}";

    @Value("${topic.cart-cleared}")
    private String cartClearedTopic;

    @Value("${topic.cart-item-added}")
    private String carAddedTopic;

    @Value("${topic.cart-item-removed}")
    private String cartRemovedTopic;

    @Value("${topic.cart-checkout}")
    private String cartCheckoutTopic;

    public void sendCartClearedEvent(CartClearedEvent event) {
        log.info("Publishing Cart Cleared event: {}", event);

        try {
            kafkaTemplate.send(cartClearedTopic, event);
        } catch (Exception ex) {
            log.error(FALLBACK_ERROR, ex.getMessage());
            fallbackService.enqueueFailedEvent(event);
        }

    }

    public void sendAddToCartEvent(AddToCartEvent event) {
        log.info("Publishing Add to Cart event: {}", event);
        try {
            kafkaTemplate.send(carAddedTopic, event);
        } catch(Exception ex) {
            log.error(FALLBACK_ERROR, ex.getMessage());
            fallbackService.enqueueFailedEvent(event);
        }
    }

    public void sendRemoveFromCartEvent(RemoveFromCartEvent event) {
        log.info("Publishing Remove from Cart  event: {}", event);

        try {
            kafkaTemplate.send(cartRemovedTopic, event);
        } catch(Exception ex) {
            log.error(FALLBACK_ERROR, ex.getMessage());
            fallbackService.enqueueFailedEvent(event);
        }
    }

    public void sendCheckoutEvent(CheckoutEvent event) {
        log.info("Publishing Checkout event: {}", event);

        try {
            kafkaTemplate.send(cartCheckoutTopic, event);
        } catch (Exception ex) {
            log.error(FALLBACK_ERROR, ex.getMessage());
            fallbackService.enqueueFailedEvent(event);
        }
    }
}
