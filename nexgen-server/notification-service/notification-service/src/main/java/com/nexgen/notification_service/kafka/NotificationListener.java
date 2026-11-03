package com.nexgen.notification_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexgen.notification_service.dto.CheckoutEvent;
import com.nexgen.notification_service.dto.OrderCreatedEvent;
import com.nexgen.notification_service.dto.OrderLifecycleEvent;
import com.nexgen.notification_service.dto.UserAccountEvent;
import com.nexgen.notification_service.entity.Notification;
import com.nexgen.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * The customer-facing end of the event flows.
 *
 * <p>Every notification is keyed by the {@code eventId} of the event that caused it,
 * so a redelivered event cannot send the customer the same message twice. Delivery
 * (email/SMS/push) is deliberately left to a later integration; this service records
 * what would be delivered, which is what makes the consumer verifiable.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "${topic.order-created}", groupId = "notification-service")
    public void onOrderCreated(ConsumerRecord<String, String> record) {
        OrderCreatedEvent event = read(record, OrderCreatedEvent.class);
        if (event == null) {
            return;
        }
        String total = event.getTotalAmount() == null ? "" : " totalling " + event.getTotalAmount();
        record(event.getEventId(), "ORDER_RECEIVED", event.getUserId(), event.getOrderNumber(),
                "Order received",
                "Thanks! We received order " + event.getOrderNumber() + total
                        + ". We are reserving your items now.");
    }

    @KafkaListener(topics = "${topic.order-confirmed}", groupId = "notification-service")
    public void onOrderConfirmed(ConsumerRecord<String, String> record) {
        OrderLifecycleEvent event = read(record, OrderLifecycleEvent.class);
        if (event == null) {
            return;
        }
        record(event.getEventId(), "ORDER_CONFIRMED", event.getUserId(), event.getOrderNumber(),
                "Order confirmed",
                "Good news: order " + event.getOrderNumber() + " is confirmed and being prepared.");
    }

    @KafkaListener(topics = "${topic.order-cancelled}", groupId = "notification-service")
    public void onOrderCancelled(ConsumerRecord<String, String> record) {
        OrderLifecycleEvent event = read(record, OrderLifecycleEvent.class);
        if (event == null) {
            return;
        }
        String reason = event.getReason() == null ? "" : " Reason: " + event.getReason() + ".";
        record(event.getEventId(), "ORDER_CANCELLED", event.getUserId(), event.getOrderNumber(),
                "Order cancelled",
                "Order " + event.getOrderNumber() + " has been cancelled." + reason);
    }

    @KafkaListener(topics = "${topic.cart-checkout}", groupId = "notification-service")
    public void onCheckoutAccepted(ConsumerRecord<String, String> record) {
        CheckoutEvent event = read(record, CheckoutEvent.class);
        if (event == null) {
            return;
        }
        record(event.getEventId(), "CHECKOUT_ACCEPTED", event.getUserId(), event.getOrderNumber(),
                "Checkout accepted",
                "We are confirming availability for order " + event.getOrderNumber() + ".");
    }

    @KafkaListener(topics = "${topic.order-status-updated}", groupId = "notification-service")
    public void onOrderStatusUpdated(ConsumerRecord<String, String> record) {
        OrderLifecycleEvent event = read(record, OrderLifecycleEvent.class);
        if (event == null) {
            return;
        }
        String status = event.getStatus() == null ? "updated" : event.getStatus();
        record(event.getEventId(), "ORDER_STATUS_UPDATED", event.getUserId(), event.getOrderNumber(),
                "Order " + status.toLowerCase(),
                "Order " + event.getOrderNumber() + " is now " + status + ".");
    }

    @KafkaListener(topics = "${topic.user-registration}", groupId = "notification-service")
    public void onUserRegistered(ConsumerRecord<String, String> record) {
        UserAccountEvent event = read(record, UserAccountEvent.class);
        if (event == null) {
            return;
        }
        record(event.getEventId(), "WELCOME", event.getUserId(), null,
                "Welcome to NexGen",
                "Welcome, " + event.getUsername() + "! Your account is ready.");
    }

    @KafkaListener(topics = "${topic.user-login}", groupId = "notification-service")
    public void onUserLogin(ConsumerRecord<String, String> record) {
        UserAccountEvent event = read(record, UserAccountEvent.class);
        if (event == null) {
            return;
        }
        String ip = event.getIpAddress() == null ? "an unknown device" : event.getIpAddress();
        record(event.getEventId(), "LOGIN", event.getUserId(), null,
                "New sign-in",
                "New sign-in to your account from " + ip + ".");
    }

    @KafkaListener(topics = "${topic.user-profile-updated}", groupId = "notification-service")
    public void onProfileUpdated(ConsumerRecord<String, String> record) {
        UserAccountEvent event = read(record, UserAccountEvent.class);
        if (event == null) {
            return;
        }
        record(event.getEventId(), "PROFILE_UPDATED", event.getUserId(), null,
                "Profile updated",
                "Your profile details were updated.");
    }

    @KafkaListener(topics = "${topic.user-password-changed}", groupId = "notification-service")
    public void onPasswordChanged(ConsumerRecord<String, String> record) {
        UserAccountEvent event = read(record, UserAccountEvent.class);
        if (event == null) {
            return;
        }
        record(event.getEventId(), "PASSWORD_CHANGED", event.getUserId(), null,
                "Password changed",
                "Your password was changed. If this was not you, contact support immediately.");
    }

    private void record(String eventId, String type, String userId, String orderNumber,
                        String subject, String message) {
        if (eventId == null || eventId.isBlank()) {
            log.warn("Ignoring {} notification without an eventId", type);
            return;
        }
        if (notificationRepository.existsByEventId(eventId)) {
            log.debug("Notification for event {} already recorded", eventId);
            return;
        }
        notificationRepository.save(Notification.builder()
                .eventId(eventId)
                .userId(userId)
                .orderNumber(orderNumber)
                .type(type)
                .subject(subject)
                .message(message)
                .createdAt(Instant.now())
                .build());
        log.info("NOTIFY {} ({}): {} [event {}]", type, userId, subject, eventId);
    }

    private <T> T read(ConsumerRecord<String, String> record, Class<T> type) {
        if (record.value() == null) {
            log.warn("Skipping empty record from {}", record.topic());
            return null;
        }
        try {
            return objectMapper.readValue(record.value(), type);
        } catch (Exception e) {
            // Fail loudly so DefaultErrorHandler retries and finally dead-letters it.
            throw new IllegalArgumentException("Unreadable " + type.getSimpleName() + " from " + record.topic(), e);
        }
    }
}
