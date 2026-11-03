package com.nexgen.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Consumer-side view of {@code order-confirmed}, {@code order-cancelled} and
 * {@code order-status-updated}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderLifecycleEvent {

    private String eventId;

    private String eventType;

    private String orderNumber;

    private String userId;

    private String status;

    private Long timestamp;

    private String correlationId;

    private String reason;
}
