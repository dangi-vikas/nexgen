package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.InventoryRejectedEvent;
import com.nexgen.inventory_service.dto.InventoryReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Publishes the outcome of a stock reservation. These are the events that make the
 * checkout a real saga: the order service only confirms or cancels an order after
 * seeing {@code inventory-reserved} or {@code inventory-rejected}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryOutcomePublisher {

    static final String INVENTORY_RESERVED = "inventory-reserved";
    static final String INVENTORY_REJECTED = "inventory-rejected";

    private final OutboxService outboxService;

    public String reserved(String orderNumber, String correlationId, List<InventoryReservedEvent.Item> items) {
        String eventId = UUID.randomUUID().toString();
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .eventId(eventId)
                .eventType(INVENTORY_RESERVED)
                .orderNumber(orderNumber)
                .correlationId(correlationId)
                .occurredAt(Instant.now())
                .items(items)
                .build();
        outboxService.enqueue(eventId, "RESERVATION", orderNumber, INVENTORY_RESERVED, "1.0",
                "inventory-service", correlationId, null, event);
        log.info("Reserved stock for order {} (event {})", orderNumber, eventId);
        return eventId;
    }

    public String rejected(String orderNumber, String correlationId, String reason) {
        String eventId = UUID.randomUUID().toString();
        InventoryRejectedEvent event = InventoryRejectedEvent.builder()
                .eventId(eventId)
                .eventType(INVENTORY_REJECTED)
                .orderNumber(orderNumber)
                .correlationId(correlationId)
                .occurredAt(Instant.now())
                .reason(reason)
                .build();
        outboxService.enqueue(eventId, "RESERVATION", orderNumber, INVENTORY_REJECTED, "1.0",
                "inventory-service", correlationId, null, event);
        log.warn("Rejected reservation for order {} (event {}): {}", orderNumber, eventId, reason);
        return eventId;
    }
}
