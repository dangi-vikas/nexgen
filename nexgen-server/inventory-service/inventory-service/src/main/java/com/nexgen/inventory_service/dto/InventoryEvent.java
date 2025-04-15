package com.nexgen.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryEvent implements Serializable {
    private String skuCode;
    private String action;
    private int quantity;
    private Instant eventTime;
}
