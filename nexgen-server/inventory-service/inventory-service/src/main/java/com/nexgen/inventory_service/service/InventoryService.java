package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.entity.InventoryItem;

import java.util.List;

public interface InventoryService {
    InventoryItem createItem(InventoryItem item);
    InventoryItem updateItem(String skuCode, InventoryItem updatedItem);
    void deleteItem(String skuCode);
    InventoryItem getItemBySkuCode(String skuCode);
    List<InventoryItem> getAllItems();
}
