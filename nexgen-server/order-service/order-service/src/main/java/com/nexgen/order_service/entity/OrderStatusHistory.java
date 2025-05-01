package com.nexgen.order_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNumber;

    @Enumerated(EnumType.STRING)
    private OrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private OrderStatus newStatus;

    private Instant changedAt;

    private String changedBy;
}
