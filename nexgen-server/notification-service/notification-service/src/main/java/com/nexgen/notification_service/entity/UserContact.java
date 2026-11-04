package com.nexgen.notification_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Address book entry, built from user-registration and user-profile-updated. */
@Entity
@Table(name = "user_contacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContact {

    @Id
    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
