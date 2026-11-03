package com.nexgen.order_service.client;

import com.nexgen.order_service.dto.OrderItemRequest;

import java.util.List;

public interface InventoryClient {
    void reserve(String orderNumber, List<OrderItemRequest> items, String authorizationHeader);

    void release(String orderNumber, String authorizationHeader);
}
