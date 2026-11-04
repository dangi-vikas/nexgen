package com.nexgen.notification_service.repository;

import com.nexgen.notification_service.entity.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContactRepository extends JpaRepository<UserContact, String> {
}
