package com.nexgen.inventory_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Value("${topic.inventory-events:inventory-events-topic}")
    private String inventoryEvents;

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(inventoryEvents).partitions(3).replicas(1).build();
    }
}
