package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.InventoryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.mockito.Mockito.*;

class KafkaInventoryProducerTest {

    @Mock
    private KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    @InjectMocks
    private KafkaInventoryProducer kafkaInventoryProducer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kafkaInventoryProducer = new KafkaInventoryProducer(kafkaTemplate);
        try {
            var field = KafkaInventoryProducer.class.getDeclaredField("inventoryTopic");
            field.setAccessible(true);
            field.set(kafkaInventoryProducer, "test-topic");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testSendInventoryEvent_ShouldSendMessageToKafkaTopic() {
        InventoryEvent event = InventoryEvent.builder()
                .action("CREATED")
                .skuCode("ABC123")
                .quantity(10)
                .eventTime(Instant.parse("2024-04-14T12:00:00Z"))
                .build();

        kafkaInventoryProducer.sendInventoryEvent(event);

        verify(kafkaTemplate, times(1)).send("test-topic", event);
    }
}
