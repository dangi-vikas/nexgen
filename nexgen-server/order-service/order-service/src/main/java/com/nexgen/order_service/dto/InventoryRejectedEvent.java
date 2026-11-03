package com.nexgen.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Payload of {@code inventory-rejected}, published by the inventory service. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryRejectedEvent {

    private String eventId;

    private String eventType;

    private String orderNumber;

    private String correlationId;

    private Instant occurredAt;

    /** Why the reservation could not be made, e.g. "Insufficient stock for SKU: SKU-1". */
    private String reason;
}
