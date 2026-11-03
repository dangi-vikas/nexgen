package com.nexgen.product_service.service;

import com.nexgen.product_service.dto.ProductEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

class KafkaProducerServiceTest {

    @Mock
    private OutboxService outboxService;

    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kafkaProducerService = new KafkaProducerService(outboxService);
    }

    private ProductEvent sampleEvent() {
        return ProductEvent.builder()
                .skuCode("SKU123")
                .name("TestProduct")
                .quantity(50)
                .eventType("CREATED")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Test
    void sendProductCreatedEvent_enqueuesProductCreated() {
        ProductEvent event = sampleEvent();

        kafkaProducerService.sendProductCreatedEvent(event);

        ArgumentCaptor<String> eventId = ArgumentCaptor.forClass(String.class);
        verify(outboxService).enqueue(eventId.capture(), eq("PRODUCT"), eq("SKU123"),
                eq("product-created"), eq("1.0"), eq("product-service"), isNull(), isNull(), eq(event));
        assertNotNull(eventId.getValue());
        assertEquals(eventId.getValue(), event.getEventId(), "outbox id and payload eventId must match");
    }

    @Test
    void sendProductStockUpdatedEvent_enqueuesProductUpdated() {
        kafkaProducerService.sendProductStockUpdatedEvent(sampleEvent());

        verify(outboxService).enqueue(anyString(), anyString(), anyString(), eq("product-updated"),
                anyString(), anyString(), any(), any(), any(ProductEvent.class));
    }

    @Test
    void sendProductDeletedEvent_enqueuesProductDeleted() {
        kafkaProducerService.sendProductDeletedEvent(sampleEvent());

        verify(outboxService).enqueue(anyString(), anyString(), anyString(), eq("product-deleted"),
                anyString(), anyString(), any(), any(), any(ProductEvent.class));
    }
}
