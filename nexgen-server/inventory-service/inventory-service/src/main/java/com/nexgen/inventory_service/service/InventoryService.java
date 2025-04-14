package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface InventoryService {
    InventoryItem createItem(InventoryItem item);
    InventoryItem updateItem(String skuCode, InventoryItem updatedItem);
    void deleteItem(String skuCode);
    InventoryItem getItemBySkuCode(String skuCode);
    Page<InventoryItem> getAllItems(Pageable pageable);
}
