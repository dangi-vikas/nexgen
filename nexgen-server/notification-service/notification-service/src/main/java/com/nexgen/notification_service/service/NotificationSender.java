package com.nexgen.notification_service.service;

import com.nexgen.notification_service.entity.Notification;

/**
 * Delivery port. The SMTP implementation works with Mailpit locally and with any
 * SMTP relay in production; an HTTP API (Resend/Postmark) would be another
 * implementation, not a change to the consumer.
 */
public interface NotificationSender {

    /** @return the provider's message id, if it has one */
    String send(Notification notification) throws Exception;
}
