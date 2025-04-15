package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.InventoryEvent;
import com.nexgen.inventory_service.entity.InventoryItem;
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
        InventoryItem item = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new RuntimeException("Item not found with SKU: " + skuCode));

        item.setName(updatedItem.getName());
        item.setQuantity(updatedItem.getQuantity());
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
                .orElseThrow(() -> new RuntimeException("Item not found with SKU: " + skuCode));
        repository.delete(item);
    }

    @Override
    @Cacheable(value = "inventory", key = "#skuCode")
    public InventoryItem getItemBySkuCode(String skuCode) {
        return repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new RuntimeException("Item not found with SKU: " + skuCode));
    }

    @Override
    public Page<InventoryItem> getAllItems(Pageable pageable) {
        return repository.findAll(pageable);
    }
}
