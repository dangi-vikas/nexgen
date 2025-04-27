package com.nexgen.order_service.service;

import com.nexgen.order_service.dto.OrderRequest;
import com.nexgen.order_service.dto.OrderResponse;
import com.nexgen.order_service.dto.PagedOrderResponse;

public interface OrderService {
    OrderResponse createOrder(OrderRequest orderRequest);
    OrderResponse getOrderById(String orderNumber);
    PagedOrderResponse getOrdersByUserId(String userId, int page, int size);
    void cancelOrder(String orderNumber);
}
