package com.nexgen.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Customer notification service: a pure Kafka consumer. It owns no search or
 * checkout logic; it reacts to the events other services publish and records the
 * message it would deliver (email, push, SMS).
 */
// Without this the @Scheduled delivery job never runs and notifications sit PENDING.
@EnableScheduling
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
