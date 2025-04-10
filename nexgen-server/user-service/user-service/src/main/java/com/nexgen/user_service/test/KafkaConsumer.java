package com.nexgen.user_service.test;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    @KafkaListener(topics = "user-registration", groupId = "nexgen")
    public void listenRegistration(String message) {
        System.out.println("🟢 Received registration message: " + message);
    }

    @KafkaListener(topics = "user-login", groupId = "nexgen")
    public void listenLogin(String message) {
        System.out.println("🔵 Received login message: " + message);
    }

    @KafkaListener(topics = "user-profile-updated", groupId = "nexgen")
    public void listenProfileUpdated(String message) {
        System.out.println("🟡 Received profile update message: " + message);
    }

    @KafkaListener(topics = "user-password-changed", groupId = "nexgen")
    public void listenPasswordChanged(String message) {
        System.out.println("🟠 Received password change message: " + message);
    }

}
