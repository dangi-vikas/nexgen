package com.nexgen.inventory_service.repository;

import com.nexgen.inventory_service.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySkuCode(String skuCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InventoryItem i set i.reservedQuantity = i.reservedQuantity + :qty, i.version = coalesce(i.version, 0) + 1 where i.skuCode = :skuCode and (coalesce(i.quantity, 0) - coalesce(i.reservedQuantity, 0)) >= :qty")
    int reserve(@Param("skuCode") String skuCode, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InventoryItem i set i.reservedQuantity = i.reservedQuantity - :qty, i.version = coalesce(i.version, 0) + 1 where i.skuCode = :skuCode and coalesce(i.reservedQuantity, 0) >= :qty")
    int release(@Param("skuCode") String skuCode, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InventoryItem i set i.quantity = i.quantity - :qty, i.reservedQuantity = i.reservedQuantity - :qty, i.version = coalesce(i.version, 0) + 1 where i.skuCode = :skuCode and coalesce(i.reservedQuantity, 0) >= :qty and coalesce(i.quantity, 0) >= :qty")
    int confirm(@Param("skuCode") String skuCode, @Param("qty") int qty);

    /**
     * Puts stock back that a CONFIRMED reservation already deducted. Releasing it
     * instead would decrement a reserved quantity the confirmation already consumed.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InventoryItem i set i.quantity = i.quantity + :qty, i.version = coalesce(i.version, 0) + 1 where i.skuCode = :skuCode")
    int restock(@Param("skuCode") String skuCode, @Param("qty") int qty);
}
