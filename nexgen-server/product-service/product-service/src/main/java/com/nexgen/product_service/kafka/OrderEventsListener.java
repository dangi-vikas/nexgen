package com.nexgen.product_service.kafka;

import com.nexgen.product_service.entity.ProcessedEvent;
import com.nexgen.product_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventsListener {
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "order-created", groupId = "product-service")
    @Transactional
    public void onOrderCreated(Object payload) {
        String eventId = String.valueOf(payload.hashCode());
        if (processedEventRepository.existsById(eventId)) {
            log.debug("Duplicate ignored");
            return;
        }
        log.info("Product service consumed order-created: {}", payload);
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType("order-created")
                .processedAt(Instant.now())
                .build());
    }
}
