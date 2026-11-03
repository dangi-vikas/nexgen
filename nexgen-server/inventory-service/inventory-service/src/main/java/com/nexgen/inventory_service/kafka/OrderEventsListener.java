package com.nexgen.inventory_service.kafka;

import com.nexgen.inventory_service.entity.ProcessedEvent;
import com.nexgen.inventory_service.repository.ProcessedEventRepository;
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

    @KafkaListener(topics = "order-created", groupId = "inventory-service")
    @Transactional
    public void onOrderCreated(Object payload) {
        String eventId = String.valueOf(payload.hashCode());
        if (processedEventRepository.existsById(eventId)) {
            log.debug("Duplicate order-created ignored");
            return;
        }
        log.info("Inventory handling order-created: {}", payload);
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType("order-created")
                .processedAt(Instant.now())
                .build());
    }

    @KafkaListener(topics = "order-cancelled", groupId = "inventory-service")
    @Transactional
    public void onOrderCancelled(Object payload) {
        String eventId = String.valueOf(payload.hashCode());
        if (processedEventRepository.existsById(eventId)) {
            log.debug("Duplicate order-cancelled ignored");
            return;
        }
        log.info("Inventory releasing stock for cancelled order: {}", payload);
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType("order-cancelled")
                .processedAt(Instant.now())
                .build());
    }
}
