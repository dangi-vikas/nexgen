package com.nexgen.order_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "event_type", length = 100)
    private String eventType;
}
