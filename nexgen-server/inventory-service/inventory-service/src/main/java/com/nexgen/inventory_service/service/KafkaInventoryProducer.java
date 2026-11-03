package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.InventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Enqueues stock-level events ({@code inventory-events-topic}) into the outbox.
 * The product service consumes them; nothing in this service consumes its own
 * events any more.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaInventoryProducer {

    static final String INVENTORY_EVENTS = "inventory-events";

    private final OutboxService outboxService;

    public String sendInventoryEvent(InventoryEvent event) {
        return sendInventoryEvent(event, null);
    }

    public String sendInventoryEvent(InventoryEvent event, String correlationId) {
        String eventId = event.getEventId() == null ? UUID.randomUUID().toString() : event.getEventId();
        event.setEventId(eventId);
        outboxService.enqueue(eventId, "INVENTORY", event.getSkuCode(), INVENTORY_EVENTS, "1.0",
                "inventory-service", correlationId, null, event);
        log.info("Enqueued inventory event {} for SKU {} ({})", eventId, event.getSkuCode(), event.getAction());
        return eventId;
    }
}
