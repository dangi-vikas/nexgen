package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.ReservationRequest;
import com.nexgen.inventory_service.dto.ReservationResponse;

public interface InventoryReservationService {
    ReservationResponse reserve(ReservationRequest request);

    void release(String orderNumber);

    void confirm(String orderNumber);
}
