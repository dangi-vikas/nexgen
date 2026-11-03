package com.nexgen.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${topic.order-confirmed:order-confirmed}")
    private String orderConfirmed;

    @Value("${topic.order-cancelled:order-cancelled}")
    private String orderCancelled;

    @Value("${topic.payment-authorized:payment-authorized}")
    private String paymentAuthorized;

    @Value("${topic.payment-failed:payment-failed}")
    private String paymentFailed;

    @Value("${topic.payment-refunded:payment-refunded}")
    private String paymentRefunded;

    @Bean
    public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(orderConfirmed).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(orderCancelled).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentAuthorizedTopic() {
        return TopicBuilder.name(paymentAuthorized).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(paymentFailed).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentRefundedTopic() {
        return TopicBuilder.name(paymentRefunded).partitions(3).replicas(1).build();
    }
}
