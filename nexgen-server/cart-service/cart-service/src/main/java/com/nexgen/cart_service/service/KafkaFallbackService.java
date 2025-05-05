package com.nexgen.cart_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaFallbackService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_FAILED_EVENT_QUEUE = "FAILED_KAFKA_EVENTS";

    public void enqueueFailedEvent(Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            String eventType = event.getClass().getSimpleName();
            String composite = eventType + "::" + json;
            redisTemplate.opsForList().rightPush(REDIS_FAILED_EVENT_QUEUE, composite);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Kafka event for Redis fallback", e);
        }
    }


    public List<String> getFailedEvents(int max) {
        return redisTemplate.opsForList()
                .range(REDIS_FAILED_EVENT_QUEUE, 0, max - 1);
    }

    public void removeProcessedEvents(int count) {
        for (int i = 0; i < count; i++) {
            redisTemplate.opsForList().leftPop(REDIS_FAILED_EVENT_QUEUE);
        }
    }
}
