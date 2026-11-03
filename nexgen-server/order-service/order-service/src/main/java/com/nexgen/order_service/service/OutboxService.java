package com.nexgen.order_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.order_service.entity.OutboxEvent;
import com.nexgen.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void enqueue(String aggregateType, String aggregateId, String eventType, String eventVersion,
                        String producer, String correlationId, String causationId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventVersion(eventVersion == null ? "1.0" : eventVersion)
                    .occurredAt(Instant.now())
                    .producer(producer)
                    .correlationId(correlationId)
                    .causationId(causationId)
                    .payload(json)
                    .status("PENDING")
                    .retryCount(0)
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to enqueue outbox event", e);
            throw new RuntimeException(e);
        }
    }
}
