package com.nexgen.user_service.service;

import com.nexgen.user_service.dto.UserAccountEvent;
import com.nexgen.user_service.dto.UserLoginEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private OutboxService outboxService;

    private KafkaProducerService service;

    @BeforeEach
    void setUp() {
        service = new KafkaProducerService(outboxService);
        // The real writer returns the id it stored; the mock must do the same so the
        // producer's returned id can be compared with the enqueued one.
        when(outboxService.enqueue(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registrationEventIsTypedJsonKeyedByUsername() {
        String eventId = service.sendUserRegistrationEvent("john", "john@example.com");

        ArgumentCaptor<UserAccountEvent> payload = ArgumentCaptor.forClass(UserAccountEvent.class);
        verify(outboxService).enqueue(eq(eventId), eq("USER"), eq("john"), eq("user-registration"),
                eq("1.0"), eq("user-service"), isNull(), isNull(), payload.capture());

        assertEquals("user-registration", payload.getValue().getEventType());
        assertEquals("john", payload.getValue().getUserId());
        assertEquals("john@example.com", payload.getValue().getEmail());
        assertEquals(eventId, payload.getValue().getEventId());
    }

    @Test
    void loginEventCarriesIpAddressAndEventId() {
        String eventId = service.sendLoginEvent(UserLoginEvent.builder()
                .username("john")
                .ipAddress("127.0.0.1")
                .loginTime(Instant.parse("2026-01-01T00:00:00Z"))
                .build());

        ArgumentCaptor<UserLoginEvent> payload = ArgumentCaptor.forClass(UserLoginEvent.class);
        verify(outboxService).enqueue(eq(eventId), eq("USER"), eq("john"), eq("user-login"),
                eq("1.0"), eq("user-service"), isNull(), isNull(), payload.capture());

        assertEquals("127.0.0.1", payload.getValue().getIpAddress());
        assertEquals("john", payload.getValue().getUserId());
        assertEquals("user-login", payload.getValue().getEventType());
    }
}
