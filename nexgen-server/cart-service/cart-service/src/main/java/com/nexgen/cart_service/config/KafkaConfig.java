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

    @Bean
    public NewTopic cartCheckoutTopic() {
        return TopicBuilder.name(cartCheckout).partitions(3).replicas(1).build();
    }
}
