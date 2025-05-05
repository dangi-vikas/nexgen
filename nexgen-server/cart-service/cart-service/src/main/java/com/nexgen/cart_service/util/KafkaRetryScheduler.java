package com.nexgen.cart_service.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.cart_service.dto.AddToCartEvent;
import com.nexgen.cart_service.dto.CartClearedEvent;
import com.nexgen.cart_service.dto.CheckoutEvent;
import com.nexgen.cart_service.dto.RemoveFromCartEvent;
import com.nexgen.cart_service.service.KafkaFallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaRetryScheduler {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaFallbackService fallbackService;
    private final ObjectMapper objectMapper;

    @Value("${topic.cart-cleared}")
    private String cartClearedTopic;

    @Value("${topic.cart-item-added}")
    private String cartAddedTopic;

    @Value("${topic.cart-item-removed}")
    private String cartRemovedTopic;

    @Value("${topic.cart-checkout}")
    private String cartCheckoutTopic;

    @Scheduled(fixedDelay = 60000)
    public void retryFromRedisQueue() {
        List<String> events = fallbackService.getFailedEvents(10);
        int successfullyProcessed = 0;

        for (String composite : events) {
            try {
                String[] parts = composite.split("::", 2);
                if (parts.length != 2) continue;

                String type = parts[0];
                String json = parts[1];

                switch (type) {
                    case "AddToCartEvent" -> {
                        AddToCartEvent event = objectMapper.readValue(json, AddToCartEvent.class);
                        kafkaTemplate.send(cartAddedTopic, event.getUserId(), event);
                    }
                    case "RemoveFromCartEvent" -> {
                        RemoveFromCartEvent event = objectMapper.readValue(json, RemoveFromCartEvent.class);
                        kafkaTemplate.send(cartRemovedTopic, event.getUserId(), event);
                    }
                    case "CartClearedEvent" -> {
                        CartClearedEvent event = objectMapper.readValue(json, CartClearedEvent.class);
                        kafkaTemplate.send(cartClearedTopic, event.getUserId(), event);
                    }
                    case "CheckoutEvent" -> {
                        CheckoutEvent event = objectMapper.readValue(json, CheckoutEvent.class);
                        kafkaTemplate.send(cartCheckoutTopic, event.getUserId(), event);
                    }
                    default -> log.warn("Unknown event type: {}", type);
                }

                successfullyProcessed++;
            } catch (Exception e) {
                log.warn("Retry failed, will try again later", e);
                break;
            }
        }

        fallbackService.removeProcessedEvents(successfullyProcessed);
    }
}
