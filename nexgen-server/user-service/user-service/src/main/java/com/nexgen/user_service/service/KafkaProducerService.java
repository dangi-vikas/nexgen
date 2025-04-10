package com.nexgen.user_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.user_service.dto.UserLoginEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String REGISTRATION_TOPIC = "user-registration";
    private static final String LOGIN_TOPIC = "user-login";
    private static final String UPDATE_PROFILE_TOPIC = "user-profile-updated";
    public static final String PASSWORD_CHANGED_TOPIC = "user-password-changed";


    public void sendUserRegistrationEvent(String username) {
        kafkaTemplate.send(REGISTRATION_TOPIC, "New user registered: " + username);
    }

    public void sendLoginEvent(UserLoginEvent loginEvent) {
        try {
            String message = objectMapper.writeValueAsString(loginEvent);
            kafkaTemplate.send(LOGIN_TOPIC, message);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing login event: " + e.getMessage());
        }
    }

    public void sendProfileUpdatedEvent(String username) {
        kafkaTemplate.send(UPDATE_PROFILE_TOPIC, "User profile updated: " + username);
    }

    public void sendPasswordChangedEvent(String username) {
        kafkaTemplate.send(PASSWORD_CHANGED_TOPIC, "User password changed: " + username);
    }
}

