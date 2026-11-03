package com.nexgen.notification_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_notifications_event_id", columnNames = "event_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The event that produced this notification; also the idempotency key. */
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "order_number", length = 100)
    private String orderNumber;

    /** ORDER_RECEIVED, ORDER_CONFIRMED, ORDER_CANCELLED, WELCOME, LOGIN, PROFILE_UPDATED, PASSWORD_CHANGED. */
    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 200)
    private String subject;

    @Column(length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
