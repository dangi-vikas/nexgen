package com.nexgen.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Consumer-side view of {@code order-created}. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreatedEvent {

    private String eventId;

    private String eventType;

    private String orderNumber;

    private String userId;

    private BigDecimal totalAmount;
}
