package com.nexgen.user_service.service;

import com.nexgen.user_service.dto.ChangePasswordRequest;
import com.nexgen.user_service.dto.UpdateProfileRequest;
import com.nexgen.user_service.dto.UserProfileResponse;
import com.nexgen.user_service.dto.UserRegistrationRequest;
import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.exception.DuplicateUserException;
import com.nexgen.user_service.exception.InvalidPasswordException;
import com.nexgen.user_service.exception.InvalidRoleChangeException;
import com.nexgen.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    /** Roles an administrator may assign; nothing else can be written to the column. */
    private static final Set<String> ASSIGNABLE_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducerService kafkaProducer;
    private final RedisService redisService;

    @Transactional
    public UserProfileResponse registerUser(UserRegistrationRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DuplicateUserException("Username already exists");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail().trim())
                // Roles are never accepted from the client.
                .role(DEFAULT_ROLE)
                .build();

        User savedUser = userRepository.save(user);
        kafkaProducer.sendUserRegistrationEvent(savedUser.getUsername(), savedUser.getEmail());
        return toProfile(savedUser);
    }

    @Transactional
    public void updateUserProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setEmail(request.getEmail().trim());
        userRepository.save(user);
        kafkaProducer.sendProfileUpdatedEvent(username, user.getEmail());
    }

    @Transactional
    public void changeUserPassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // A password change invalidates every refresh session for this user.
        redisService.deleteRefreshToken(username);
        kafkaProducer.sendPasswordChangedEvent(username);
    }

    /**
     * Promotes or demotes another user. Only administrators can reach this method, and
     * their own role is off limits so one account cannot lock the system out.
     */
    @Transactional
    public UserProfileResponse changeUserRole(String targetUsername, String role, String actor) {
        if (!ASSIGNABLE_ROLES.contains(role)) {
            throw new InvalidRoleChangeException("Unsupported role: " + role);
        }

        User user = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getUsername().equals(actor)) {
            throw new InvalidRoleChangeException("Administrators cannot change their own role");
        }

        String previous = user.getRole();
        if (role.equals(previous)) {
            return toProfile(user);
        }

        user.setRole(role);
        userRepository.save(user);

        // Refresh tokens were minted with the old role, so they must not be reusable.
        redisService.deleteRefreshToken(user.getUsername());

        log.info("Role changed by {}: user={} {} -> {}", actor, user.getUsername(), previous, role);
        return toProfile(user);
    }

    /**
     * Used by authentication. It must not be cached: a password or role change
     * has to take effect on the next login/authorization decision.
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UserProfileResponse getUserProfile(String username) {
        return toProfile(getUserByUsername(username));
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole());
    }
}
