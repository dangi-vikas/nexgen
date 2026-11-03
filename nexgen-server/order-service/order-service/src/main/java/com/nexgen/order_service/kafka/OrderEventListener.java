package com.nexgen.order_service.kafka;

import com.nexgen.order_service.entity.ProcessedEvent;
import com.nexgen.order_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "order-events", groupId = "order-service")
    @Transactional
    public void listen(Object payload) {
        String eventId = String.valueOf(payload.hashCode());
        if (processedEventRepository.existsById(eventId)) {
            log.debug("Duplicate event ignored: {}", eventId);
            return;
        }
        try {
            log.info("Order service consumed event: {}", payload);
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(payload.getClass().getSimpleName())
                    .processedAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.error("Failed to process order event", e);
            throw e;
        }
    }
}
