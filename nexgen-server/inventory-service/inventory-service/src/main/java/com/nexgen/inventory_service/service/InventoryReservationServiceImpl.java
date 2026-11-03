package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.InventoryEvent;
import com.nexgen.inventory_service.dto.InventoryReservedEvent;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns stock reservation.
 *
 * <p>Reserving, releasing and confirming all happen here and publish their outcome
 * to the outbox inside the same transaction, so the reservation table and the
 * events sent to the order service cannot drift apart.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryOutcomePublisher outcomePublisher;
    private final KafkaInventoryProducer inventoryEventProducer;

    @Override
    @CacheEvict(value = "inventory", allEntries = true)
    public ReservationResponse reserve(ReservationRequest request) {
        if (request == null || request.getOrderNumber() == null || request.getOrderNumber().isBlank()) {
            throw new InventoryReservationException("orderNumber is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InventoryReservationException("items are required");
        }

        String orderNumber = request.getOrderNumber();
        String correlationId = request.getCorrelationId();

        if (reservationRepository.existsByOrderNumber(orderNumber)) {
            // Already reserved (redelivery or a retry after a crash between the
            // reservation commit and the processed-event marker). Re-announce the
            // outcome, because the previous announcement may never have been sent.
            List<InventoryReservation> existing = reservationRepository.findByOrderNumber(orderNumber);
            List<InventoryReservedEvent.Item> items = existing.stream()
                    .map(r -> InventoryReservedEvent.Item.builder()
                            .skuCode(r.getSkuCode())
                            .quantity(r.getQuantity())
                            .build())
                    .toList();
            outcomePublisher.reserved(orderNumber, correlationId, items);
            return ReservationResponse.builder()
                    .orderNumber(orderNumber)
                    .status("RESERVED")
                    .items(existing.stream()
                            .map(r -> ReservationItemRequest.builder()
                                    .skuCode(r.getSkuCode())
                                    .quantity(r.getQuantity())
                                    .build())
                            .toList())
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
                throw new InsufficientStockException("Insufficient stock for SKU: " + item.getSkuCode()
                        + " (available " + inventoryItem.getAvailableQuantity() + ")");
            }

            InventoryReservation reservation = InventoryReservation.builder()
                    .orderNumber(orderNumber)
                    .skuCode(item.getSkuCode())
                    .quantity(item.getQuantity())
                    .status(ReservationStatus.RESERVED)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .version(0L)
                    .build();
            reservationRepository.save(reservation);
            reservedItems.add(ReservationItemRequest.builder()
                    .skuCode(item.getSkuCode())
                    .quantity(item.getQuantity())
                    .build());
        }

        outcomePublisher.reserved(orderNumber, correlationId, reservedItems.stream()
                .map(i -> InventoryReservedEvent.Item.builder()
                        .skuCode(i.getSkuCode())
                        .quantity(i.getQuantity())
                        .build())
                .toList());

        return ReservationResponse.builder()
                .orderNumber(orderNumber)
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
            log.info("No reservation to release for order {}", orderNumber);
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
            publishStockLevel(reservation.getSkuCode());
        }
    }

    @Override
    @CacheEvict(value = "inventory", allEntries = true)
    public void confirm(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new InventoryReservationException("orderNumber is required");
        }
        List<InventoryReservation> reservations =
                reservationRepository.findByOrderNumberAndStatus(orderNumber, ReservationStatus.RESERVED);
        if (reservations == null || reservations.isEmpty()) {
            log.info("No reservation to confirm for order {}", orderNumber);
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
            publishStockLevel(reservation.getSkuCode());
        }
    }

    private void publishStockLevel(String skuCode) {
        inventoryRepository.findBySkuCode(skuCode).ifPresent(item ->
                inventoryEventProducer.sendInventoryEvent(new InventoryEvent(
                        null,
                        item.getSkuCode(),
                        "UPDATED",
                        item.getQuantity() == null ? 0 : item.getQuantity(),
                        Instant.now())));
    }
}
