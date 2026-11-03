package com.nexgen.user_service.service;

import com.nexgen.user_service.security.TokenHashing;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final StringRedisTemplate redisTemplate;

    public void blacklistToken(String token, long expiryMillis) {
        if (token == null || token.isBlank() || expiryMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(
                TokenHashing.sha256Hex(token),
                "blacklisted",
                Duration.ofMillis(expiryMillis));
    }

    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(TokenHashing.sha256Hex(token)));
    }
}
