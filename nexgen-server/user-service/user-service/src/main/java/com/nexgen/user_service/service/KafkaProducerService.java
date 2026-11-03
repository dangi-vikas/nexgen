package com.nexgen.user_service.service;

import com.nexgen.user_service.dto.UserAccountEvent;
import com.nexgen.user_service.dto.UserLoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Enqueues account events into the transactional outbox.
 *
 * <p>The previous implementation published string concatenations such as
 * {@code "New user registered: john"} with a hard-coded {@code localhost:9092}
 * producer and no key, so no consumer could react to them. These are now typed
 * JSON events keyed by username, consumed by the notification service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    static final String USER_REGISTRATION = "user-registration";
    static final String USER_LOGIN = "user-login";
    static final String USER_PROFILE_UPDATED = "user-profile-updated";
    static final String USER_PASSWORD_CHANGED = "user-password-changed";

    private final OutboxService outboxService;

    public String sendUserRegistrationEvent(String username) {
        return enqueueAccountEvent(USER_REGISTRATION, username);
    }

    public String sendProfileUpdatedEvent(String username) {
        return enqueueAccountEvent(USER_PROFILE_UPDATED, username);
    }

    public String sendPasswordChangedEvent(String username) {
        return enqueueAccountEvent(USER_PASSWORD_CHANGED, username);
    }

    public String sendLoginEvent(UserLoginEvent event) {
        String eventId = event.getEventId() == null ? UUID.randomUUID().toString() : event.getEventId();
        event.setEventId(eventId);
        event.setEventType(USER_LOGIN);
        if (event.getUserId() == null) {
            event.setUserId(event.getUsername());
        }
        log.info("Enqueuing Login event {} for user {}", eventId, event.getUsername());
        return outboxService.enqueue(eventId, "USER", event.getUsername(), USER_LOGIN, "1.0",
                "user-service", null, null, event);
    }

    private String enqueueAccountEvent(String eventType, String username) {
        String eventId = UUID.randomUUID().toString();
        UserAccountEvent event = UserAccountEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .userId(username)
                .username(username)
                .occurredAt(Instant.now())
                .build();
        log.info("Enqueuing {} event {} for user {}", eventType, eventId, username);
        return outboxService.enqueue(eventId, "USER", username, eventType, "1.0",
                "user-service", null, null, event);
    }
}
