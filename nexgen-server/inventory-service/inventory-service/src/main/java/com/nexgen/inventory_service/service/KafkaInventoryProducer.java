package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.InventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaInventoryProducer {

    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    @Value("${topic.inventory-events}")
    private String inventoryTopic;

    public void sendInventoryEvent(InventoryEvent event) {
        kafkaTemplate.send(inventoryTopic, event);
        log.info("📦 Published inventory event to topic [{}]: {}", inventoryTopic, event);
    }
}
