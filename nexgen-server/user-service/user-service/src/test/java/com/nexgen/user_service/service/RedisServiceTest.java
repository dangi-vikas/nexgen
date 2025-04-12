package com.nexgen.user_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class RedisServiceTest {
    @Autowired
    private RedisService redisService;

    @Test
    void testRefreshTokenCRUD() {
        String username = "john";
        String token = "ref-token-123";

        redisService.saveRefreshToken(username, token);

        String savedToken = redisService.getRefreshToken(username);
        assertEquals(token, savedToken);

        redisService.deleteRefreshToken(username);

        String deletedToken = redisService.getRefreshToken(username);
        assertNull(deletedToken);
    }
}
