package com.nexgen.inventory_service.service;


import com.nexgen.inventory_service.entity.InventoryItem;
import com.nexgen.inventory_service.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository repository;

    @Override
    public InventoryItem createItem(InventoryItem item) {
        return repository.save(item);
    }

    @Override
    public InventoryItem updateItem(String skuCode, InventoryItem updatedItem) {
        InventoryItem item = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new RuntimeException("Item not found with SKU: " + skuCode));
        item.setName(updatedItem.getName());
        item.setQuantity(updatedItem.getQuantity());
        return repository.save(item);
    }

    @Override
    public void deleteItem(String skuCode) {
        InventoryItem item = repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new RuntimeException("Item not found with SKU: " + skuCode));
        repository.delete(item);
    }

    @Override
    public InventoryItem getItemBySkuCode(String skuCode) {
        return repository.findBySkuCode(skuCode)
                .orElseThrow(() -> new RuntimeException("Item not found with SKU: " + skuCode));
    }

    @Override
    public List<InventoryItem> getAllItems() {
        return repository.findAll();
    }
}
