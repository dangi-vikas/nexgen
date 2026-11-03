package com.nexgen.cart_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Value("${topic.cart-checkout:cart-checkout}")
    private String cartCheckout;

    @Value("${topic.order-confirmed:order-confirmed}")
    private String orderConfirmed;

    @Value("${topic.product-deleted:product-deleted}")
    private String productDeleted;

    @Bean
    public NewTopic cartCheckoutTopic() {
        return TopicBuilder.name(cartCheckout).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(orderConfirmed).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name(productDeleted).partitions(3).replicas(1).build();
    }
}
