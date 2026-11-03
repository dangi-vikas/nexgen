package com.nexgen.user_service.exception;

import com.nexgen.user_service.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void accessDeniedMapsToForbidden() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(403, response.getStatusCode().value());
        assertEquals("FORBIDDEN", response.getBody().getErrorCode());
        assertEquals("Access is denied", response.getBody().getMessage());
    }

    @Test
    void genericFailureDoesNotLeakTheExceptionMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGeneral(new IllegalStateException("sensitive internal detail"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("sensitive"));
    }
}
