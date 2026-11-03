package com.nexgen.order_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Value("${topic.order-created:order-created}")
    private String orderCreated;

    @Value("${topic.order-confirmed:order-confirmed}")
    private String orderConfirmed;

    @Value("${topic.order-cancelled:order-cancelled}")
    private String orderCancelled;

    @Value("${topic.order-status-updated:order-status-updated}")
    private String orderStatusUpdated;

    @Value("${topic.inventory-reserved:inventory-reserved}")
    private String inventoryReserved;

    @Value("${topic.inventory-rejected:inventory-rejected}")
    private String inventoryRejected;

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
    public NewTopic orderStatusUpdatedTopic() {
        return TopicBuilder.name(orderStatusUpdated).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryReservedTopic() {
        return TopicBuilder.name(inventoryReserved).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryRejectedTopic() {
        return TopicBuilder.name(inventoryRejected).partitions(3).replicas(1).build();
    }
}
