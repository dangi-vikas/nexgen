package com.nexgen.product_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisCacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final long DEFAULT_TTL = 10 * 60; // 10 minutes

    public void cache(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    public void cache(String key, Object value) {
        cache(key, value, DEFAULT_TTL);
    }

    public <T> T getCached(String key, Class<T> type) {
        Object cached = redisTemplate.opsForValue().get(key);
        if (type.isInstance(cached)) {
            return type.cast(cached);
        }
        return null;
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }
}
