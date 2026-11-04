package com.nexgen.notification_service.service;

import com.nexgen.notification_service.entity.Notification;
import com.nexgen.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Turns the notification table into a delivery outbox: rows written PENDING by the
 * Kafka listener are sent here, with bounded retries.
 *
 * <p>A missing recipient is not a delivery failure — the contact address may simply
 * not have been seen yet; it stays PENDING until it does.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notifications.delivery.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationDeliveryJob {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;

    @Value("${notifications.delivery.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${notifications.delivery.delay:5000}")
    public void deliverPending() {
        List<Notification> pending = notificationRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        for (Notification notification : pending) {
            if (notification.getRecipient() == null || notification.getRecipient().isBlank()) {
                log.debug("No recipient yet for {} notification, leaving it pending", notification.getType());
                continue;
            }
            try {
                String providerMessageId = notificationSender.send(notification);
                notification.setStatus("SENT");
                notification.setSentAt(Instant.now());
                notification.setProviderMessageId(providerMessageId);
                notification.setLastError(null);
            } catch (Exception e) {
                int attempts = notification.getAttempts() + 1;
                notification.setAttempts(attempts);
                notification.setLastError(truncate(e.getMessage()));
                if (attempts >= maxAttempts) {
                    notification.setStatus("FAILED");
                    log.error("Giving up on {} notification after {} attempts", notification.getType(), attempts, e);
                } else {
                    log.warn("Delivery of {} notification failed, attempt {}/{}",
                            notification.getType(), attempts, maxAttempts, e);
                }
            }
            notificationRepository.save(notification);
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
