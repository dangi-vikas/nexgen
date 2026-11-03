package com.nexgen.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Payload of {@code inventory-rejected}, consumed by the order service. */
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

    private String reason;
}
