package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.ReservationItemRequest;
import com.nexgen.inventory_service.dto.ReservationRequest;
import com.nexgen.inventory_service.dto.ReservationResponse;
import com.nexgen.inventory_service.entity.InventoryItem;
import com.nexgen.inventory_service.entity.InventoryReservation;
import com.nexgen.inventory_service.entity.ReservationStatus;
import com.nexgen.inventory_service.exception.InsufficientStockException;
import com.nexgen.inventory_service.exception.InventoryReservationException;
import com.nexgen.inventory_service.exception.ItemNotFoundException;
import com.nexgen.inventory_service.repository.InventoryRepository;
import com.nexgen.inventory_service.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    @Override
    @CacheEvict(value = "inventory", allEntries = true)
    public ReservationResponse reserve(ReservationRequest request) {
        if (request == null || request.getOrderNumber() == null || request.getOrderNumber().isBlank()) {
            throw new InventoryReservationException("orderNumber is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InventoryReservationException("items are required");
        }

        if (reservationRepository.existsByOrderNumber(request.getOrderNumber())) {
            List<InventoryReservation> existing = reservationRepository.findByOrderNumber(request.getOrderNumber());
            List<ReservationItemRequest> items = existing.stream()
                    .map(r -> ReservationItemRequest.builder()
                            .skuCode(r.getSkuCode())
                            .quantity(r.getQuantity())
                            .build())
                    .toList();
            return ReservationResponse.builder()
                    .orderNumber(request.getOrderNumber())
                    .status("RESERVED")
                    .items(items)
                    .build();
        }

        List<ReservationItemRequest> reservedItems = new ArrayList<>();
        for (ReservationItemRequest item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new InventoryReservationException("quantity must be greater than 0");
            }
            InventoryItem inventoryItem = inventoryRepository.findBySkuCode(item.getSkuCode())
                    .orElseThrow(() -> new ItemNotFoundException("Item not found with SKU: " + item.getSkuCode()));

            int updated = inventoryRepository.reserve(item.getSkuCode(), item.getQuantity());
            if (updated == 0) {
                throw new InsufficientStockException("Insufficient stock for SKU: " + item.getSkuCode());
            }

            InventoryReservation reservation = InventoryReservation.builder()
                    .orderNumber(request.getOrderNumber())
                    .skuCode(item.getSkuCode())
                    .quantity(item.getQuantity())
                    .status(ReservationStatus.RESERVED)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .version(0L)
                    .build();
            reservationRepository.save(reservation);
            reservedItems.add(ReservationItemRequest.builder().skuCode(item.getSkuCode()).quantity(item.getQuantity()).build());
        }

        return ReservationResponse.builder()
                .orderNumber(request.getOrderNumber())
                .status("RESERVED")
                .items(reservedItems)
                .build();
    }

    @Override
    @CacheEvict(value = "inventory", allEntries = true)
    public void release(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new InventoryReservationException("orderNumber is required");
        }
        List<InventoryReservation> reservations = reservationRepository.findByOrderNumber(orderNumber);
        if (reservations == null || reservations.isEmpty()) {
            return;
        }
        for (InventoryReservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.RELEASED) {
                continue;
            }
            int updated = inventoryRepository.release(reservation.getSkuCode(), reservation.getQuantity());
            if (updated == 0) {
                throw new InventoryReservationException("Unable to release reservation for order: " + orderNumber);
            }
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setUpdatedAt(Instant.now());
            reservationRepository.save(reservation);
        }
    }

    @Override
    @CacheEvict(value = "inventory", allEntries = true)
    public void confirm(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new InventoryReservationException("orderNumber is required");
        }
        List<InventoryReservation> reservations = reservationRepository.findByOrderNumberAndStatus(orderNumber, ReservationStatus.RESERVED);
        if (reservations == null || reservations.isEmpty()) {
            return;
        }
        for (InventoryReservation reservation : reservations) {
            int updated = inventoryRepository.confirm(reservation.getSkuCode(), reservation.getQuantity());
            if (updated == 0) {
                throw new InventoryReservationException("Unable to confirm reservation for order: " + orderNumber);
            }
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setUpdatedAt(Instant.now());
            reservationRepository.save(reservation);
        }
    }
}
