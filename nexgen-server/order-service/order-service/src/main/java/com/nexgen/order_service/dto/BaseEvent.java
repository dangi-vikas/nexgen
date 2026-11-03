package com.nexgen.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseEvent {
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private String aggregateId;
    private String aggregateType;
    @Builder.Default
    private String schemaVersion = "1.0";
    @Builder.Default
    private Instant occurredAt = Instant.now();
    private String producer;
    private String correlationId;
    private String causationId;
} 
