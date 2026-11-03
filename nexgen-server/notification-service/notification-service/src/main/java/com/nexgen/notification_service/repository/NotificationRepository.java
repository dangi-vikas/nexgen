package com.nexgen.notification_service.repository;

import com.nexgen.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventId(String eventId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
}
