package com.nexgen.product_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.product_service.dto.InventoryLevelEvent;
import com.nexgen.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the product catalogue's quantity in step with the stock ledger.
 *
 * <p>Inventory is the source of truth for stock: it publishes {@code inventory-events}
 * whenever a reservation is confirmed or a reservation is released. This listener
 * mirrors the level onto the product row so the catalogue does not drift. It never
 * publishes back, which is what stops an event loop between the two services.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryEventsListener {

    private final ObjectMapper objectMapper;
    private final ProcessedEventGuard guard;
    private final ProductRepository productRepository;

    @KafkaListener(topics = "${topic.inventory-events}", groupId = "product-service")
    @Transactional
    public void onInventoryChanged(ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            log.warn("Skipping empty record from {}", record.topic());
            return;
        }

        InventoryLevelEvent event;
        try {
            event = objectMapper.readValue(record.value(), InventoryLevelEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unreadable inventory event from " + record.topic(), e);
        }

        if (event.getEventId() != null && guard.isProcessed(event.getEventId())) {
            log.debug("Duplicate inventory event {} ignored", event.getEventId());
            return;
        }

        productRepository.findBySkuCode(event.getSkuCode()).ifPresentOrElse(product -> {
            product.setQuantity(event.getQuantity());
            productRepository.save(product);
            log.info("Mirrored stock level for {} to {} ({})", event.getSkuCode(), event.getQuantity(), event.getAction());
        }, () -> log.debug("No product for SKU {}, ignoring stock event", event.getSkuCode()));

        if (event.getEventId() != null) {
            guard.markProcessed(event.getEventId(), "inventory-events");
        }
    }
}
