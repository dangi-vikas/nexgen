package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.entity.OutboxEvent;
import com.nexgen.inventory_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.publish.delay:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusOrderByOccurredAtAsc("PENDING");
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload());
                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() != null && event.getRetryCount() > 10) {
                    event.setStatus("FAILED");
                }
                outboxEventRepository.save(event);
                log.warn("Failed to publish outbox event {}", event.getId(), e);
            }
        }
    }
}
