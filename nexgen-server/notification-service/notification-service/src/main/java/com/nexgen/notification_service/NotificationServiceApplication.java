package com.nexgen.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Customer notification service: a pure Kafka consumer. It owns no search or
 * checkout logic; it reacts to the events other services publish and records the
 * message it would deliver (email, push, SMS).
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
