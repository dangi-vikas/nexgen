package com.nexgen.notification_service.service;

import com.nexgen.notification_service.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends the notification as a plain-text email through the configured SMTP host. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${notifications.from:no-reply@nexgen.local}")
    private String from;

    @Override
    public String send(Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notification.getRecipient());
        message.setSubject(notification.getSubject() == null ? "NexGen" : notification.getSubject());
        message.setText(notification.getMessage() == null ? "" : notification.getMessage());
        mailSender.send(message);
        log.info("Sent {} notification to {}", notification.getType(), notification.getRecipient());
        return null;
    }
}
