package com.nexgen.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Consumer-side view of the order lifecycle events. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderLifecycleEvent {

    private String eventId;

    private String eventType;

    private String orderNumber;

    private String userId;

    private String status;

    private String reason;
}
