package com.nexgen.inventory_service.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String skuCode;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Version
    @Builder.Default
    private Long version = 0L;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
        if (quantity == null) {
            quantity = 0;
        }
        if (version == null) {
            version = 0L;
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
        if (quantity == null) {
            quantity = 0;
        }
        updatedAt = LocalDateTime.now();
    }

    public int getAvailableQuantity() {
        int q = quantity == null ? 0 : quantity;
        int r = reservedQuantity == null ? 0 : reservedQuantity;
        int available = q - r;
        return available < 0 ? 0 : available;
    }
}
