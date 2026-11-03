package com.nexgen.user_service.service;

import com.nexgen.user_service.security.TokenHashing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RedisService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USED_REFRESH_PREFIX = "refresh_used:";

    /**
     * Atomically compare the presented refresh hash with the stored value and
     * replace it. A previously used token is recorded so a replay can revoke
     * the single current session. An unknown token returns 0 without touching
     * the valid session, so a random invalid token cannot cause a logout.
     *
     * KEYS[1] = current refresh key, KEYS[2] = used-token marker key
     * ARGV[1] = presented hash, ARGV[2] = replacement hash, ARGV[3] = TTL ms
     */
    private static final RedisScript<Long> ROTATE_SCRIPT = RedisScript.of("""
            local current = redis.call('GET', KEYS[1])
            if current == ARGV[1] then
              redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
              redis.call('SET', KEYS[2], '1', 'PX', ARGV[3])
              return 1
            end
            if redis.call('EXISTS', KEYS[2]) == 1 then
              redis.call('DEL', KEYS[1])
              return -1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final long refreshTtlMs;

    public RedisService(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-ttl-ms:604800000}") long refreshTtlMs) {
        if (refreshTtlMs <= 0) {
            throw new IllegalStateException("Refresh token lifetime must be positive");
        }
        this.redisTemplate = redisTemplate;
        this.refreshTtlMs = refreshTtlMs;
    }

    /**
     * Stores a hash of the refresh token. Raw tokens are never persisted.
     */
    public void saveRefreshToken(String username, String refreshToken) {
        String key = key(username);
        redisTemplate.opsForValue().set(
                key,
                TokenHashing.sha256Hex(refreshToken),
                Duration.ofMillis(refreshTtlMs));
    }

    public void deleteRefreshToken(String username) {
        redisTemplate.delete(key(username));
    }

    /**
     * Atomically rotates a refresh token. Exactly one caller can replace the
     * current value. A replay of an already-used token revokes the current
     * session; an unknown token is rejected without deleting valid state.
     */
    public RotationResult rotateRefreshToken(String username,
                                             String presentedToken,
                                             String replacementToken) {
        String presentedHash = TokenHashing.sha256Hex(presentedToken);
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(key(username), USED_REFRESH_PREFIX + presentedHash),
                presentedHash,
                TokenHashing.sha256Hex(replacementToken),
                String.valueOf(refreshTtlMs));

        if (result == null) {
            return RotationResult.INVALID;
        }
        return switch (result.intValue()) {
            case 1 -> RotationResult.SUCCESS;
            case -1 -> RotationResult.REUSE_DETECTED;
            default -> RotationResult.INVALID;
        };
    }

    private String key(String username) {
        return REFRESH_TOKEN_PREFIX + username;
    }

    public enum RotationResult {
        SUCCESS,
        INVALID,
        REUSE_DETECTED
    }
}
