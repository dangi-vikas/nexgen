package com.nexgen.user_service.exception;

/**
 * Raised when a token is missing, expired, tampered with, of the wrong type,
 * or no longer matches server-side session state.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
