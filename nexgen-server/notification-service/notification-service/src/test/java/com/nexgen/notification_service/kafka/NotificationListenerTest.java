package com.nexgen.notification_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.notification_service.entity.Notification;
import com.nexgen.notification_service.repository.NotificationRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationListener(new ObjectMapper(), notificationRepository);
    }

    private ConsumerRecord<String, String> record(String topic, String json) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", json);
    }

    @Test
    void orderCreatedProducesOneOrderReceivedNotification() {
        when(notificationRepository.existsByEventId("evt-1")).thenReturn(false);

        listener.onOrderCreated(record("order-created", """
                {"eventId":"evt-1","eventType":"order-created","orderNumber":"ORD-1","userId":"user1",
                 "totalAmount":42.50,"items":[{"skuCode":"SKU-1","quantity":1}]}
                """));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("evt-1", captor.getValue().getEventId());
        assertEquals("ORDER_RECEIVED", captor.getValue().getType());
        assertEquals("user1", captor.getValue().getUserId());
        assertEquals("ORD-1", captor.getValue().getOrderNumber());
        assertTrue(captor.getValue().getMessage().contains("ORD-1"));
    }

    @Test
    void redeliveredEventDoesNotNotifyTwice() {
        when(notificationRepository.existsByEventId("evt-1")).thenReturn(true);

        listener.onOrderCreated(record("order-created", """
                {"eventId":"evt-1","eventType":"order-created","orderNumber":"ORD-1","userId":"user1"}
                """));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void cancellationCarriesTheReason() {
        when(notificationRepository.existsByEventId("evt-2")).thenReturn(false);

        listener.onOrderCancelled(record("order-cancelled", """
                {"eventId":"evt-2","eventType":"order-cancelled","orderNumber":"ORD-2","userId":"user1",
                 "reason":"Insufficient stock for SKU: SKU-1"}
                """));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("ORDER_CANCELLED", captor.getValue().getType());
        assertTrue(captor.getValue().getMessage().contains("Insufficient stock"));
    }
}
