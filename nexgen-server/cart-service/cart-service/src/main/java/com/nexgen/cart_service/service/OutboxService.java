package com.nexgen.cart_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.cart_service.entity.OutboxEvent;
import com.nexgen.cart_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(String aggregateType, String aggregateId, String eventType, Object payload, String correlationId) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventVersion("1.0")
                    .occurredAt(Instant.now())
                    .producer("cart-service")
                    .correlationId(correlationId)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status("PENDING")
                    .retryCount(0)
                    .build();
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
