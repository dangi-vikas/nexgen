package com.nexgen.cart_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    private String id;

    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String eventVersion;
    private Instant occurredAt;
    private String producer;
    private String correlationId;
    private String causationId;
    @Column(length = 4000)
    private String payload;
    private String status;
    private int retryCount;
    private Instant publishedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (eventVersion == null) {
            eventVersion = "1.0";
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
