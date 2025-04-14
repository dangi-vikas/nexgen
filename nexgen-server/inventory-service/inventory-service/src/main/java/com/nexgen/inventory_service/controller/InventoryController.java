package com.nexgen.inventory_service.controller;

import com.nexgen.inventory_service.entity.InventoryItem;
import com.nexgen.inventory_service.service.InventoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Controller", description = "Inventory management APIs")
@CrossOrigin()
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryItem> create(@RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.createItem(item));
    }

    @PutMapping("/{skuCode}")
    public ResponseEntity<InventoryItem> update(@PathVariable String skuCode,
                                                @RequestBody InventoryItem updatedItem) {
        return ResponseEntity.ok(inventoryService.updateItem(skuCode, updatedItem));
    }

    @DeleteMapping("/{skuCode}")
    public ResponseEntity<Void> delete(@PathVariable String skuCode) {
        inventoryService.deleteItem(skuCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{skuCode}")
    public ResponseEntity<InventoryItem> getBySkuCode(@PathVariable String skuCode) {
        return ResponseEntity.ok(inventoryService.getItemBySkuCode(skuCode));
    }

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAll() {
        return ResponseEntity.ok(inventoryService.getAllItems());
    }
}
