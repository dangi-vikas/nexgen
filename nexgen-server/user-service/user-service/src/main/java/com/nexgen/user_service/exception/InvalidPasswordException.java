package com.nexgen.user_service.exception;

/**
 * Raised when a password change cannot be completed because the supplied
 * current password is wrong or the new password is not acceptable.
 */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
