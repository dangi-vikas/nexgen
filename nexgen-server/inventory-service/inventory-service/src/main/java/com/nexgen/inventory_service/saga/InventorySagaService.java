package com.nexgen.inventory_service.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InventorySagaService {
    public void release(String orderNumber, String correlationId) {
        log.info("Releasing inventory for order {} (corr={})", orderNumber, correlationId);
    }

    public void reserve(String orderNumber) {
        log.info("Reserving inventory for order {}", orderNumber);
    }
}
