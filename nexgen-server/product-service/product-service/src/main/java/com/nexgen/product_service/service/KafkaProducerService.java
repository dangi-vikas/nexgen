package com.nexgen.product_service.service;

import com.nexgen.product_service.dto.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @Value("${topic.product-created}")
    private String productCreatedTopic;

    @Value("${topic.product-updated}")
    private String productUpdatedTopic;

    @Value("${topic.product-deleted}")
    private String productDeletedTopic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public void sendProductCreatedEvent(ProductEvent event) {
        log.info("Publishing Product Created event: {}", event);
        log.info("🚀 Kafka is connecting to: {}", bootstrapServers);
        kafkaTemplate.send(productCreatedTopic, event);
    }

    public void sendProductStockUpdatedEvent(ProductEvent event) {
        log.info("Publishing Product Updated event: {}", event);
        kafkaTemplate.send(productUpdatedTopic, event);
    }

    public void sendProductDeletedEvent(ProductEvent event) {
        log.info("Publishing Product Deleted event for ID: {}", event);
        kafkaTemplate.send(productDeletedTopic, event);
    }

}