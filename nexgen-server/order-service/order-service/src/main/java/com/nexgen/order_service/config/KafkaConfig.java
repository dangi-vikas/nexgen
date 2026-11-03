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

    @Value("${topic.order-cancelled:order-cancelled}")
    private String orderCancelled;

    @Value("${topic.order-status-updated:order-status-updated}")
    private String orderStatusUpdated;

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(orderCreated).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(orderCancelled).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderStatusUpdatedTopic() {
        return TopicBuilder.name(orderStatusUpdated).partitions(3).replicas(1).build();
    }
}
