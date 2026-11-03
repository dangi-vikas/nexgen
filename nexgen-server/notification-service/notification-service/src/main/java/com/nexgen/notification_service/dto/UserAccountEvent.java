package com.nexgen.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Consumer-side view of the {@code user-*} account events. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAccountEvent {

    private String eventId;

    private String eventType;

    private String userId;

    private String username;

    private String ipAddress;
}
