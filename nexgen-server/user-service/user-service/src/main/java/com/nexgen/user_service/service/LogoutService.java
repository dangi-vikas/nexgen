package com.nexgen.user_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final StringRedisTemplate redisTemplate;

    public void blacklistToken(String token, long expiryMillis) {
        redisTemplate.opsForValue().set(token, "blacklisted", Duration.ofMillis(expiryMillis));
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey(token);
    }
}
