package com.nexgen.user_service.service;

import com.nexgen.user_service.dto.ChangePasswordRequest;
import com.nexgen.user_service.dto.UpdateProfileRequest;
import com.nexgen.user_service.dto.UserRegistrationRequest;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.exception.DuplicateUserException;
import com.nexgen.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducerService kafkaProducer;

    public User registerUser(UserRegistrationRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateUserException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(request.getRole() != null ? request.getRole() : "ROLE_USER")
                .build();

        User savedUser = userRepository.save(user);

        kafkaProducer.sendUserRegistrationEvent(savedUser.getUsername());

        return savedUser;
    }

    @CacheEvict(value = "users", key = "#username" )
    public void updateUserProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);
        kafkaProducer.sendProfileUpdatedEvent(username);
    }

    public void changeUserPassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        kafkaProducer.sendPasswordChangedEvent(username);
    }

    @Cacheable(value = "users", key = "#username")
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

}
