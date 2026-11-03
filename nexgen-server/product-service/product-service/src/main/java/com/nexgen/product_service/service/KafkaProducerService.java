package com.nexgen.product_service.service;

import com.nexgen.product_service.dto.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Enqueues {@code product-created} / {@code product-updated} / {@code product-deleted}
 * into the transactional outbox.
 *
 * <p>Inventory consumes these to create or remove its stock rows and to follow stock
 * changes; cart consumes {@code product-deleted} to purge the product from carts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    static final String PRODUCT_CREATED = "product-created";
    static final String PRODUCT_UPDATED = "product-updated";
    static final String PRODUCT_DELETED = "product-deleted";

    private final OutboxService outboxService;

    public String sendProductCreatedEvent(ProductEvent event) {
        return enqueue(event, PRODUCT_CREATED, "Product Created");
    }

    public String sendProductStockUpdatedEvent(ProductEvent event) {
        return enqueue(event, PRODUCT_UPDATED, "Product Updated");
    }

    public String sendProductDeletedEvent(ProductEvent event) {
        return enqueue(event, PRODUCT_DELETED, "Product Deleted");
    }

    private String enqueue(ProductEvent event, String eventType, String description) {
        String eventId = event.getEventId() == null ? UUID.randomUUID().toString() : event.getEventId();
        event.setEventId(eventId);
        log.info("Enqueuing {} event {} for SKU {}", description, eventId, event.getSkuCode());
        return outboxService.enqueue(eventId, "PRODUCT", event.getSkuCode(), eventType, "1.0",
                "product-service", null, null, event);
    }
}
