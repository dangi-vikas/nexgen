package com.nexgen.order_service.service;

import com.nexgen.order_service.dto.OrderRequest;
import com.nexgen.order_service.dto.OrderResponse;
import com.nexgen.order_service.dto.PagedOrderResponse;
import com.nexgen.order_service.entity.OrderStatus;

public interface OrderService {
    OrderResponse createOrder(OrderRequest orderRequest);
    OrderResponse getOrderById(String orderNumber);
    PagedOrderResponse getOrdersByUserId(String userId, int page, int size, OrderStatus status);
    void cancelOrder(String orderNumber);
    void updateOrderStatus(String orderNumber, OrderStatus newStatus);
}
