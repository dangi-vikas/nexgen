package com.nexgen.user_service.service;

import com.nexgen.user_service.dto.UserProfileResponse;
import com.nexgen.user_service.dto.UserRegistrationRequest;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.exception.DuplicateUserException;
import com.nexgen.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String PASSWORD = "StrongPass!123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaProducerService kafkaProducer;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest registrationRequest() {
        return new UserRegistrationRequest("john", PASSWORD, "john@example.com");
    }

    @Test
    void registrationAlwaysAssignsUserRoleAndNeverExposesPassword() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserProfileResponse response = userService.registerUser(registrationRequest());

        assertEquals(1L, response.getId());
        assertEquals("john", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("ROLE_USER", response.getRole());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("ROLE_USER", captor.getValue().getRole());
        assertEquals("encoded-password", captor.getValue().getPassword());
        verify(kafkaProducer).sendUserRegistrationEvent("john");
    }

    @Test
    void duplicateUsernameIsRejected() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(new User()));

        assertThrows(DuplicateUserException.class, () -> userService.registerUser(registrationRequest()));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void profileLookupReturnsDto() {
        User user = User.builder()
                .id(2L)
                .username("john")
                .email("john@example.com")
                .role("ROLE_USER")
                .password("encoded")
                .build();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile("john");

        assertEquals(2L, response.getId());
        assertEquals("john", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("ROLE_USER", response.getRole());
    }

    @Test
    void missingProfileRaisesUsernameNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> userService.getUserProfile("missing"));
    }
}
