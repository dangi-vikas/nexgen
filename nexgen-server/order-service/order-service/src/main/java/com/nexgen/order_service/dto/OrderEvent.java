package com.nexgen.order_service.dto;

import com.nexgen.order_service.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderEvent {
    private String orderNumber;
    private String userId;
    private OrderStatus status;
    private Long timestamp;
}
