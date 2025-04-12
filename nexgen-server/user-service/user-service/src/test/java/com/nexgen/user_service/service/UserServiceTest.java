package com.nexgen.user_service.service;

import com.nexgen.user_service.dto.ChangePasswordRequest;
import com.nexgen.user_service.dto.UpdateProfileRequest;
import com.nexgen.user_service.dto.UserRegistrationRequest;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaProducerService kafkaProducer;

    @InjectMocks
    private UserService userService;

    @Test
    void testRegisterUserSuccess() {
        UserRegistrationRequest request = new UserRegistrationRequest("john", "pass", "john@example.com", null);

        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded-pass");

        User savedUser = User.builder()
                .username("john")
                .password("encoded-pass")
                .email("john@example.com")
                .role("ROLE_USER")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(request);

        assertNotNull(result);
        assertEquals("john", result.getUsername());
        assertEquals("encoded-pass", result.getPassword());
        assertEquals("ROLE_USER", result.getRole());

        verify(kafkaProducer).sendUserRegistrationEvent("john");
    }

    @Test
    void testRegisterUserWhenUsernameExists() {
        UserRegistrationRequest request = new UserRegistrationRequest("john", "pass", "john@example.com", null);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(new User()));

        assertThrows(RuntimeException.class, () -> userService.registerUser(request));
        verify(kafkaProducer, never()).sendUserRegistrationEvent(anyString());
    }

    @Test
    void testUpdateUserProfileSuccess() {
        String username = "john";
        UpdateProfileRequest request = new UpdateProfileRequest("newemail@example.com");
        User existingUser = User.builder()
                .username(username)
                .email("oldemail@example.com")
                .password("encodedPass")
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        userService.updateUserProfile(username, request);

        assertEquals("newemail@example.com", existingUser.getEmail());
        verify(kafkaProducer).sendProfileUpdatedEvent(username);
    }

    @Test
    void testChangeUserPasswordSuccess() {
        String username = "john";
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");
        User existingUser = User.builder()
                .username(username)
                .password("encodedOldPass")
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("oldPass", "encodedOldPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");

        userService.changeUserPassword(username, request);

        assertEquals("encodedNewPass", existingUser.getPassword());
        verify(userRepository).save(existingUser);
        verify(kafkaProducer).sendPasswordChangedEvent(username);
    }

    @Test
    void testChangeUserPasswordWrongOldPassword() {
        String username = "john";
        ChangePasswordRequest request = new ChangePasswordRequest("wrongOld", "newPass");
        User existingUser = User.builder()
                .username(username)
                .password("encodedOldPass")
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongOld", "encodedOldPass")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.changeUserPassword(username, request));
    }

    @Test
    void testGetUserByUsernameSuccess() {
        String username = "john";
        User user = User.builder().username(username).build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        User result = userService.getUserByUsername(username);

        assertEquals(username, result.getUsername());
    }

    @Test
    void testGetUserByUsernameNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.getUserByUsername("unknown"));
    }

}
