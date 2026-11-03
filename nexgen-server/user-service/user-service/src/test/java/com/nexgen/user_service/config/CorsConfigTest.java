package com.nexgen.user_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void onlyConfiguredOriginsAreAllowed() {
        SecurityConfig config = new SecurityConfig();
        CorsConfigurationSource source =
                config.corsConfigurationSource("https://app.example, https://admin.example");
        CorsConfiguration cors = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/v1/users/me"));

        assertNotNull(cors);
        assertEquals(2, cors.getAllowedOrigins().size());
        assertEquals("https://app.example", cors.checkOrigin("https://app.example"));
        assertNull(cors.checkOrigin("https://evil.example"));
        assertFalse(Boolean.TRUE.equals(cors.getAllowCredentials()));
        assertTrue(cors.getAllowedMethods().contains("GET"));
        assertTrue(cors.getAllowedMethods().contains("POST"));
    }
}
