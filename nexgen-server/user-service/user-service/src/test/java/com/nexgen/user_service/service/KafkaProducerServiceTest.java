package com.nexgen.user_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class KafkaProducerServiceTest {
    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Test
    void testSendUserRegistrationEvent() {
        kafkaProducerService.sendUserRegistrationEvent("john");

        verify(kafkaTemplate, times(1))
                .send("user-registration", "New user registered: john");
    }
}
