package com.nexgen.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Payload of {@code user-registration}, {@code user-profile-updated} and
 * {@code user-password-changed}. Consumed by the notification service.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountEvent {

    private String eventId;

    private String eventType;

    /** Same value as {@code username}: the user service uses the username as identity. */
    private String userId;

    private String username;

    private String email;

    private Instant occurredAt;
}
