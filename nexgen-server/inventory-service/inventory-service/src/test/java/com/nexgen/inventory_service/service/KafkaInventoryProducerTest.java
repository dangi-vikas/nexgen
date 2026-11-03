package com.nexgen.inventory_service.service;

import com.nexgen.inventory_service.dto.InventoryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

class KafkaInventoryProducerTest {

    @Mock
    private OutboxService outboxService;

    private KafkaInventoryProducer kafkaInventoryProducer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kafkaInventoryProducer = new KafkaInventoryProducer(outboxService);
    }

    @Test
    void testSendInventoryEvent_ShouldEnqueueEventInOutbox() {
        InventoryEvent event = InventoryEvent.builder()
                .action("CREATED")
                .skuCode("ABC123")
                .quantity(10)
                .eventTime(Instant.parse("2024-04-14T12:00:00Z"))
                .build();

        kafkaInventoryProducer.sendInventoryEvent(event);

        ArgumentCaptor<String> eventId = ArgumentCaptor.forClass(String.class);
        verify(outboxService).enqueue(eventId.capture(), eq("INVENTORY"), eq("ABC123"),
                eq("inventory-events"), eq("1.0"), eq("inventory-service"), isNull(), isNull(), eq(event));
        assertNotNull(eventId.getValue());
        assertEquals(eventId.getValue(), event.getEventId(), "outbox id and payload eventId must match");
    }

    @Test
    void testSendInventoryEvent_KeepsCorrelationId() {
        InventoryEvent event = InventoryEvent.builder()
                .action("UPDATED")
                .skuCode("ABC123")
                .quantity(5)
                .eventTime(Instant.now())
                .build();

        kafkaInventoryProducer.sendInventoryEvent(event, "corr-1");

        verify(outboxService).enqueue(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), eq("corr-1"), isNull(), any(InventoryEvent.class));
    }
}
