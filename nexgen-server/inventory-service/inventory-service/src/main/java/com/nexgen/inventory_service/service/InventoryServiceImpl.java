package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.InventoryEvent;
import com.nexgen.inventory_service.entity.InventoryItem;
import com.nexgen.inventory_service.exception.DuplicateItemException;
import com.nexgen.inventory_service.exception.ItemNotFoundException;
import com.nexgen.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;
    private final KafkaInventoryProducer kafkaInventoryProducer;

    @Override
    @CachePut(value = "inventory", key = "#item.skuCode")
    public InventoryItem createItem(InventoryItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item is required");
        }
        if (item.getQuantity() != null && item.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }
        if (item.getReservedQuantity() != null && item.getReservedQuantity() < 0) {
            throw new IllegalArgumentException("Reserved quantity must be non-negative");
        }
        if (repository.findBySkuCode(item.getSkuCode()).isPresent()) {
            throw new DuplicateItemException("Item with SKU already exists: " + item.getSkuCode());
        }
        if (item.getReservedQuantity() == null) {
            item.setReservedQuantity(0);
        }
        if (item.getVersion() == null) {
            item.setVersion(0L);
        }

        InventoryItem createdItem = repository.save(item);

        InventoryEvent event = new InventoryEvent(
                item.getSkuCode(),
                "CREATED",
                item.getQuantity(),
                Instant.now()
        );

        // Send CREATED event
        kafkaInventoryProducer.sendInventoryEvent(event);

        return createdItem;
    }

    @Override
    @CachePut(value = "inventory", key = "#skuCode")
    public InventoryItem updateItem(String skuCode, InventoryItem updatedItem) {
        if (updatedItem == null) {
            throw new IllegalArgumentException("Updated item is required");
        }
        if (updatedItem.getQuantity() != null && updatedItem.getQuantity() < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }
        if (updatedItem.getReservedQuantity() != null && updatedItem.getReservedQuantity() < 0) {
            throw new IllegalArgumentException("Reserved quantity must be non-negative");
        }
        InventoryItem item = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with SKU: " + skuCode));

        if (updatedItem.getName() != null) {
            item.setName(updatedItem.getName());
        }
        if (updatedItem.getQuantity() != null) {
            Integer reserved = item.getReservedQuantity() == null ? 0 : item.getReservedQuantity();
            if (updatedItem.getQuantity() < reserved) {
                throw new IllegalArgumentException("Quantity cannot be less than reserved quantity");
            }
            item.setQuantity(updatedItem.getQuantity());
        }
        if (updatedItem.getReservedQuantity() != null) {
            item.setReservedQuantity(updatedItem.getReservedQuantity());
        }
        if (item.getReservedQuantity() != null && item.getQuantity() != null && item.getReservedQuantity() > item.getQuantity()) {
            throw new IllegalArgumentException("Reserved quantity cannot exceed quantity");
        }
        InventoryItem savedItem = repository.save(item);

        // Send "UPDATED" event
        kafkaInventoryProducer.sendInventoryEvent(
                new InventoryEvent(
                        savedItem.getSkuCode(),
                        "UPDATED",
                        savedItem.getQuantity(),
                        Instant.now()
                )
        );

        //  Send "OUT_OF_STOCK" event if quantity is 0
        if (savedItem.getQuantity() == 0) {
            kafkaInventoryProducer.sendInventoryEvent(
                    new InventoryEvent(
                            savedItem.getSkuCode(),
                            "OUT_OF_STOCK",
                            0,
                            Instant.now()
                    )
            );
        }

        return savedItem;
    }

    @Override
    @CacheEvict(value = "inventory", key = "#skuCode")
    public void deleteItem(String skuCode) {
        InventoryItem item = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with SKU: " + skuCode));
        repository.delete(item);
    }

    @Override
    @Cacheable(value = "inventory", key = "#skuCode")
    public InventoryItem getItemBySkuCode(String skuCode) {
        return repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with SKU: " + skuCode));
    }

    @Override
    public Page<InventoryItem> getAllItems(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
