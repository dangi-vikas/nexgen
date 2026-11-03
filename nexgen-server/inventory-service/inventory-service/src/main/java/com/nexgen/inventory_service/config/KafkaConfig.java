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

    @Value("${topic.inventory-reserved:inventory-reserved}")
    private String inventoryReserved;

    @Value("${topic.inventory-rejected:inventory-rejected}")
    private String inventoryRejected;

    @Value("${topic.order-created:order-created}")
    private String orderCreated;

    @Value("${topic.order-confirmed:order-confirmed}")
    private String orderConfirmed;

    @Value("${topic.order-cancelled:order-cancelled}")
    private String orderCancelled;

    @Value("${topic.product-created:product-created}")
    private String productCreated;

    @Value("${topic.product-updated:product-updated}")
    private String productUpdated;

    @Value("${topic.product-deleted:product-deleted}")
    private String productDeleted;

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(inventoryEvents).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder.name(inventoryReserved).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryRejectedTopic() {
        return TopicBuilder.name(inventoryRejected).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(orderCreated).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(orderConfirmed).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(orderCancelled).partitions(3).replicas(1).build();
    }

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
}
