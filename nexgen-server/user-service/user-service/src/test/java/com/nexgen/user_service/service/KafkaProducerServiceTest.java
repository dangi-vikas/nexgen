package com.nexgen.user_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexgen.user_service.dto.UserLoginEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void registrationEventIsPublished() {
        KafkaProducerService service = new KafkaProducerService(kafkaTemplate, new ObjectMapper());

        service.sendUserRegistrationEvent("john");

        verify(kafkaTemplate).send("user-registration", "New user registered: john");
    }

    @Test
    void loginEventIsSerializedAndPublished() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        KafkaProducerService service = new KafkaProducerService(kafkaTemplate, objectMapper);

        service.sendLoginEvent(new UserLoginEvent("john", "127.0.0.1", Instant.parse("2026-01-01T00:00:00Z")));

        verify(kafkaTemplate).send(eq("user-login"), contains("\"username\":\"john\""));
    }
}
