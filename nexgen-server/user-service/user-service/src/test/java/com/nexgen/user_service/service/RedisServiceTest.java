package com.nexgen.user_service.service;

import com.nexgen.user_service.security.TokenHashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    private static final String KEY = "refresh_token:john";
    private static final long TTL_MS = 60_000L;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisService redisService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisService = new RedisService(redisTemplate, TTL_MS);
    }

    @Test
    void refreshTokenIsStoredAsHashWithTtl() {
        redisService.saveRefreshToken("john", "raw-refresh-token");

        verify(valueOperations).set(
                KEY,
                TokenHashing.sha256Hex("raw-refresh-token"),
                Duration.ofMillis(TTL_MS));
        verify(valueOperations, never()).set(eq(KEY), eq("raw-refresh-token"), any(Duration.class));
    }

    @Test
    void rotationSucceedsAndPassesHashesToTheAtomicScript() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString())).thenReturn(1L);

        RedisService.RotationResult result =
                redisService.rotateRefreshToken("john", "old-token", "new-token");

        assertEquals(RedisService.RotationResult.SUCCESS, result);
        verify(redisTemplate).execute(any(RedisScript.class), anyList(),
                eq(TokenHashing.sha256Hex("old-token")),
                eq(TokenHashing.sha256Hex("new-token")),
                eq(String.valueOf(TTL_MS)));
    }

    @Test
    void unknownTokenIsRejectedWithoutDeletingValidSession() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString())).thenReturn(0L);

        RedisService.RotationResult result =
                redisService.rotateRefreshToken("john", "random-token", "new-token");

        assertEquals(RedisService.RotationResult.INVALID, result);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void replayIsReportedAsReuse() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString())).thenReturn(-1L);

        RedisService.RotationResult result =
                redisService.rotateRefreshToken("john", "old-token", "new-token");

        assertEquals(RedisService.RotationResult.REUSE_DETECTED, result);
    }

    @Test
    void nullScriptResultIsTreatedAsInvalid() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(),
                anyString(), anyString(), anyString())).thenReturn(null);

        RedisService.RotationResult result =
                redisService.rotateRefreshToken("john", "old-token", "new-token");

        assertEquals(RedisService.RotationResult.INVALID, result);
    }

    @Test
    void deleteRemovesSessionState() {
        redisService.deleteRefreshToken("john");
        verify(redisTemplate).delete(KEY);
    }
}
