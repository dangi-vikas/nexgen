package com.nexgen.order_service.service;

import com.nexgen.order_service.dto.OrderRequest;
import com.nexgen.order_service.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest orderRequest);
    OrderResponse getOrderById(String orderNumber);
    List<OrderResponse> getOrdersByUserId(String userId);
    void cancelOrder(String orderNumber);
}
