package com.nexgen.order_service.dto;

import com.nexgen.order_service.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private String orderNumber;
    private String userId;
    private OrderStatus status;
    private Instant createdAt;
    private List<OrderItemResponse> orderItems;
}
