package com.nexgen.inventory_service.repository;

import com.nexgen.inventory_service.entity.InventoryReservation;
import com.nexgen.inventory_service.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    List<InventoryReservation> findByOrderNumber(String orderNumber);

    List<InventoryReservation> findByOrderNumberAndStatus(String orderNumber, ReservationStatus status);

    boolean existsByOrderNumber(String orderNumber);

    Optional<InventoryReservation> findByOrderNumberAndSkuCode(String orderNumber, String skuCode);
}
