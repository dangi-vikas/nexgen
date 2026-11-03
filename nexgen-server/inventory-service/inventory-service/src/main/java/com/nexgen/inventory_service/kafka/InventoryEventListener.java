package com.nexgen.inventory_service.kafka;

import com.nexgen.inventory_service.dto.InventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryEventListener {
    @KafkaListener(topics = "${topic.inventory-events}", groupId = "inventory-service")
    public void listen(InventoryEvent event) {
        log.info("Received inventory event: {}", event);
    }
}
