package com.nexgen.payment_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.payment_service.entity.OutboxEvent;
import com.nexgen.payment_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relays committed outbox rows to Kafka, awaiting the send result so a broker
 * outage cannot mark an event PUBLISHED when nothing was delivered.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final String PENDING = "PENDING";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String FAILED = "FAILED";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${topic.payment-authorized:payment-authorized}")
    private String paymentAuthorizedTopic;

    @Value("${topic.payment-failed:payment-failed}")
    private String paymentFailedTopic;

    @Value("${topic.payment-refunded:payment-refunded}")
    private String paymentRefundedTopic;

    @Value("${outbox.publish.max-retries:10}")
    private int maxRetries;

    @Value("${outbox.publish.timeout-seconds:10}")
    private long publishTimeoutSeconds;

    @Scheduled(fixedDelayString = "${outbox.publish.delay:2000}")
    public void publishPending() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusOrderByOccurredAtAsc(PENDING);
        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            String topic = topicFor(event.getEventType());
            kafkaTemplate.send(topic, event.getAggregateId(), objectMapper.readTree(event.getPayload()))
                    .get(publishTimeoutSeconds, TimeUnit.SECONDS);
            event.setStatus(PUBLISHED);
            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            recordFailure(event, e);
        } catch (Exception e) {
            recordFailure(event, e);
        }
    }

    private void recordFailure(OutboxEvent event, Exception cause) {
        int attempts = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        event.setRetryCount(attempts);
        if (attempts >= maxRetries) {
            event.setStatus(FAILED);
            log.error("Dead-lettering outbox event {} ({}) after {} attempts",
                    event.getId(), event.getEventType(), attempts, cause);
        } else {
            log.warn("Failed to publish outbox event {} ({}), attempt {}/{}",
                    event.getId(), event.getEventType(), attempts, maxRetries, cause);
        }
        outboxEventRepository.save(event);
    }

    private String topicFor(String eventType) {
        return switch (eventType) {
            case "payment-authorized" -> paymentAuthorizedTopic;
            case "payment-failed" -> paymentFailedTopic;
            case "payment-refunded" -> paymentRefundedTopic;
            default -> throw new IllegalStateException("No topic configured for event type: " + eventType);
        };
    }
}
