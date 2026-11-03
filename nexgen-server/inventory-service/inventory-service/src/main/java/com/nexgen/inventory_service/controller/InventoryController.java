package com.nexgen.inventory_service.controller;

import com.nexgen.inventory_service.dto.ReservationRequest;
import com.nexgen.inventory_service.dto.ReservationResponse;
import com.nexgen.inventory_service.entity.InventoryItem;
import com.nexgen.inventory_service.service.InventoryReservationService;
import com.nexgen.inventory_service.service.InventoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Controller", description = "Inventory management APIs")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryReservationService reservationService;

    @PostMapping
    public ResponseEntity<InventoryItem> create(@Valid @RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.createItem(item));
    }

    @PutMapping("/{skuCode}")
    public ResponseEntity<InventoryItem> update(@PathVariable String skuCode,
                                                @Valid @RequestBody InventoryItem updatedItem) {
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
    public ResponseEntity<Page<InventoryItem>> getAll(@PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(inventoryService.getAllItems(pageable));
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.reserve(request));
    }

    @DeleteMapping("/reservations/{orderNumber}")
    public ResponseEntity<Void> release(@PathVariable String orderNumber) {
        reservationService.release(orderNumber);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reservations/{orderNumber}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String orderNumber) {
        reservationService.confirm(orderNumber);
        return ResponseEntity.noContent().build();
    }
}
