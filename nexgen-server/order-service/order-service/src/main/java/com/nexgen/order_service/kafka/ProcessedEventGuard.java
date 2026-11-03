package com.nexgen.order_service.kafka;

import com.nexgen.order_service.entity.ProcessedEvent;
import com.nexgen.order_service.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Idempotency guard for Kafka consumers. The key is the {@code eventId} carried in
 * the payload, not a hash of the payload: producers reuse the same id for every
 * redelivery of the same event, so duplicates are recognised across restarts.
 *
 * <p>Processing is at-least-once. Handlers are written to be idempotent (status
 * transitions are no-ops when already applied), and the marker is only written
 * after the handler succeeds, so a crash mid-handler re-runs the handler instead of
 * losing the event.
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
