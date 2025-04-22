package com.nexgen.product_service.service;

import com.nexgen.product_service.dto.ProductEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.*;

class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Manually set topic values since @Value won't populate in unit tests
        kafkaProducerService = new KafkaProducerService(kafkaTemplate);
        setField(kafkaProducerService, "productCreatedTopic", "product-created-topic");
        setField(kafkaProducerService, "productUpdatedTopic", "product-updated-topic");
        setField(kafkaProducerService, "productDeletedTopic", "product-deleted-topic");
        setField(kafkaProducerService, "bootstrapServers", "localhost:9092");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = KafkaProducerService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    void sendProductCreatedEvent_sendsToCorrectTopic() {
        ProductEvent event = sampleEvent();

        kafkaProducerService.sendProductCreatedEvent(event);

        verify(kafkaTemplate).send("product-created-topic", event);
    }

    @Test
    void sendProductStockUpdatedEvent_sendsToCorrectTopic() {
        ProductEvent event = sampleEvent();
        event.setEventType("STOCK_UPDATED");

        kafkaProducerService.sendProductStockUpdatedEvent(event);

        verify(kafkaTemplate).send("product-updated-topic", event);
    }

    @Test
    void sendProductDeletedEvent_sendsToCorrectTopic() {
        ProductEvent event = sampleEvent();
        event.setEventType("DELETED");

        kafkaProducerService.sendProductDeletedEvent(event);

        verify(kafkaTemplate).send("product-deleted-topic", event);
    }
}
