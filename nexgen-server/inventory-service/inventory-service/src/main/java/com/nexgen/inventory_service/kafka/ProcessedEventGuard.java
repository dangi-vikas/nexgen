package com.nexgen.inventory_service.kafka;

import com.nexgen.inventory_service.entity.ProcessedEvent;
import com.nexgen.inventory_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Idempotency guard for Kafka consumers, keyed on the {@code eventId} carried in
 * the payload rather than a hash of the payload. The marker is written only after
 * the handler succeeded, and every handler is itself idempotent, so at-least-once
 * delivery cannot double-reserve stock.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessedEventGuard {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isProcessed(String eventId) {
        return eventId != null && !eventId.isBlank() && processedEventRepository.existsById(eventId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(String eventId, String eventType) {
        if (eventId == null || eventId.isBlank() || processedEventRepository.existsById(eventId)) {
            return;
        }
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .processedAt(Instant.now())
                .build());
        log.debug("Marked event {} ({}) as processed", eventId, eventType);
    }
}
