package com.nexgen.order_service.service;

import com.nexgen.order_service.dto.OrderRequest;
import com.nexgen.order_service.dto.OrderResponse;
import com.nexgen.order_service.dto.PagedOrderResponse;
import com.nexgen.order_service.entity.OrderStatus;
import com.nexgen.order_service.entity.OrderStatusHistory;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest orderRequest, String userId);

    OrderResponse getOrderById(String orderNumber, String requesterId, boolean privileged);
    PagedOrderResponse getOrdersByUserId(String userId, int page, int size, OrderStatus status);
    void cancelOrder(String orderNumber, String requesterId, boolean privileged);
    void updateOrderStatus(String orderNumber, OrderStatus newStatus);
    List<OrderStatusHistory> getOrderHistory(String orderNumber, String requesterId, boolean privileged);

    /** Applied when inventory confirms the stock reservation. */
    void confirmAfterInventoryReservation(String orderNumber);

    /** Applied when inventory rejects the stock reservation. */
    void cancelAfterInventoryRejection(String orderNumber, String reason);
}
