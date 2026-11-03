package com.nexgen.inventory_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.inventory_service.dto.ProductEvent;
import com.nexgen.inventory_service.entity.InventoryItem;
import com.nexgen.inventory_service.exception.DuplicateItemException;
import com.nexgen.inventory_service.exception.ItemNotFoundException;
import com.nexgen.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Keeps inventory rows aligned with the product catalogue.
 *
 * <ul>
 *   <li>{@code product-created} → open a stock row for the SKU</li>
 *   <li>{@code product-updated} (STOCK_UPDATED) → adopt the catalogue's quantity</li>
 *   <li>{@code product-deleted} → drop the stock row</li>
 * </ul>
 *
 * The service methods used here publish {@code inventory-events}; the product
 * service mirrors those and never publishes back, so there is no event loop.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProductEventsListener {

    private final ObjectMapper objectMapper;
    private final ProcessedEventGuard guard;
    private final InventoryService inventoryService;

    @KafkaListener(topics = "${topic.product-created}", groupId = "inventory-service")
    public void onProductCreated(ConsumerRecord<String, String> record) {
        ProductEvent event = read(record);
        if (event == null || guard.isProcessed(event.getEventId())) {
            return;
        }
        try {
            inventoryService.createItem(InventoryItem.builder()
                    .skuCode(event.getSkuCode())
                    .name(event.getName() == null ? event.getSkuCode() : event.getName())
                    .quantity(event.getQuantity())
                    .reservedQuantity(0)
                    .version(0L)
                    .build());
            log.info("Opened stock row for SKU {} from product-created", event.getSkuCode());
        } catch (DuplicateItemException e) {
            log.debug("Stock row for SKU {} already exists", event.getSkuCode());
        }
        guard.markProcessed(event.getEventId(), "product-created");
    }

    @KafkaListener(topics = "${topic.product-updated}", groupId = "inventory-service")
    public void onProductUpdated(ConsumerRecord<String, String> record) {
        ProductEvent event = read(record);
        if (event == null || guard.isProcessed(event.getEventId())) {
            return;
        }
        if (!"STOCK_UPDATED".equals(event.getEventType())) {
            log.debug("Ignoring {} event for SKU {}", event.getEventType(), event.getSkuCode());
            return;
        }
        try {
            inventoryService.updateItem(event.getSkuCode(), InventoryItem.builder()
                    .quantity(event.getQuantity())
                    .build());
            log.info("Adopted catalogue stock {} for SKU {}", event.getQuantity(), event.getSkuCode());
        } catch (ItemNotFoundException e) {
            // The catalogue knows a product inventory has never seen: open the row.
            inventoryService.createItem(InventoryItem.builder()
                    .skuCode(event.getSkuCode())
                    .name(event.getName() == null ? event.getSkuCode() : event.getName())
                    .quantity(event.getQuantity())
                    .reservedQuantity(0)
                    .version(0L)
                    .build());
        }
        guard.markProcessed(event.getEventId(), "product-updated");
    }

    @KafkaListener(topics = "${topic.product-deleted}", groupId = "inventory-service")
    public void onProductDeleted(ConsumerRecord<String, String> record) {
        ProductEvent event = read(record);
        if (event == null || guard.isProcessed(event.getEventId())) {
            return;
        }
        try {
            inventoryService.deleteItem(event.getSkuCode());
            log.info("Removed stock row for deleted SKU {}", event.getSkuCode());
        } catch (ItemNotFoundException e) {
            log.debug("No stock row for deleted SKU {}", event.getSkuCode());
        }
        guard.markProcessed(event.getEventId(), "product-deleted");
    }

    private ProductEvent read(ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            log.warn("Skipping empty record from {}", record.topic());
            return null;
        }
        try {
            return objectMapper.readValue(record.value(), ProductEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unreadable product event from " + record.topic(), e);
        }
    }
}
