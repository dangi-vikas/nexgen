package com.nexgen.notification_service.config;

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

    @Value("${topic.cart-checkout:cart-checkout}")
    private String cartCheckout;

    @Value("${topic.user-registration:user-registration}")
    private String userRegistration;

    @Value("${topic.user-login:user-login}")
    private String userLogin;

    @Value("${topic.user-profile-updated:user-profile-updated}")
    private String userProfileUpdated;

    @Value("${topic.user-password-changed:user-password-changed}")
    private String userPasswordChanged;

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
    public NewTopic cartCheckoutTopic() {
        return TopicBuilder.name(cartCheckout).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic userRegistrationTopic() {
        return TopicBuilder.name(userRegistration).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic userLoginTopic() {
        return TopicBuilder.name(userLogin).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic userProfileUpdatedTopic() {
        return TopicBuilder.name(userProfileUpdated).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic userPasswordChangedTopic() {
        return TopicBuilder.name(userPasswordChanged).partitions(3).replicas(1).build();
    }
}
