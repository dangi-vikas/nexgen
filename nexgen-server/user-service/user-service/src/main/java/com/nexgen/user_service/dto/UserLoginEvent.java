package com.nexgen.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Payload of {@code user-login}, consumed by the notification service. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginEvent {

    private String eventId;

    private String eventType;

    private String userId;

    private String username;

    private String email;

    private String ipAddress;

    private Instant loginTime;
}
