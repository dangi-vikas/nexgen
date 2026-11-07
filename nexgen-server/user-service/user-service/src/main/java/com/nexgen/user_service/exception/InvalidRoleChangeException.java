package com.nexgen.user_service.exception;

/** A role change refused by policy, for example an administrator demoting themselves. */
public class InvalidRoleChangeException extends RuntimeException {

    public InvalidRoleChangeException(String message) {
        super(message);
    }
}
