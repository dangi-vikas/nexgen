package com.nexgen.product_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Value("${topic.product-created:product-created}")
    private String productCreated;

    @Value("${topic.product-updated:product-updated}")
    private String productUpdated;

    @Value("${topic.product-deleted:product-deleted}")
    private String productDeleted;

    @Value("${topic.inventory-events:inventory-events-topic}")
    private String inventoryEvents;

    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name(productCreated).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic productUpdatedTopic() {
        return TopicBuilder.name(productUpdated).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name(productDeleted).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(inventoryEvents).partitions(3).replicas(1).build();
    }
}
