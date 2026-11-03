package com.nexgen.inventory_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.inventory_service.entity.OutboxEvent;
import com.nexgen.inventory_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox writer. Domain events are only ever published from here
 * (via {@link OutboxPublisher}), in the transaction that changed the stock, so a
 * reservation and its {@code inventory-reserved} event cannot disagree.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxService {

    private static final String CORRELATION_ID = "correlationId";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public String enqueue(String eventId, String aggregateType, String aggregateId, String eventType,
                          String eventVersion, String producer, String correlationId, String causationId,
                          Object payload) {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .id(eventId)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventVersion(eventVersion == null ? "1.0" : eventVersion)
                    .occurredAt(Instant.now())
                    .producer(producer)
                    .correlationId(correlationId == null ? MDC.get(CORRELATION_ID) : correlationId)
                    .causationId(causationId)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status("PENDING")
                    .retryCount(0)
                    .build();
            outboxEventRepository.save(event);
            log.debug("Enqueued outbox event {} ({}) for {}", eventId, eventType, aggregateId);
            return eventId;
        } catch (Exception e) {
            log.error("Failed to enqueue outbox event of type {}", eventType, e);
            throw new RuntimeException(e);
        }
    }
}
