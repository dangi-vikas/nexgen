package com.nexgen.inventory_service.repository;

import com.nexgen.inventory_service.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySkuCode(String skuCode);
}
