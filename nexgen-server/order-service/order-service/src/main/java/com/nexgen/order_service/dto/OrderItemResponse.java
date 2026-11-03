package com.nexgen.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    private String skuCode;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal lineTotal;
}
