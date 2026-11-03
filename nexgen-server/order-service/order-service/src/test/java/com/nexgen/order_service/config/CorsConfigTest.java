package com.nexgen.order_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void onlyConfiguredOriginsAreAllowedAndCredentialsAreDisabled() {
        SecurityConfig config = new SecurityConfig(
                "01234567890123456789012345678901",
                "nexgen-user-service",
                "nexgen-api",
                "https://app.example, https://admin.example");

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/v1/orders"));

        assertNotNull(cors);
        assertEquals(2, cors.getAllowedOrigins().size());
        assertEquals("https://app.example", cors.checkOrigin("https://app.example"));
        assertNull(cors.checkOrigin("https://evil.example"));
        assertFalse(Boolean.TRUE.equals(cors.getAllowCredentials()));
        assertTrue(cors.getAllowedMethods().contains("GET"));
        assertTrue(cors.getAllowedMethods().contains("POST"));
    }
}
