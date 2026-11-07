package com.nexgen.user_service.config;

import com.nexgen.user_service.entity.User;
import com.nexgen.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator so the system can be operated without editing the
 * database by hand.
 *
 * <p>It is deliberately one-shot and idempotent: an existing username is never
 * modified (no silent password reset, no silent promotion), and the runner does
 * nothing at all unless both variables are set. From then on, administrators are
 * managed through {@code PATCH /api/v1/users/{username}/role}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${users.bootstrap.admin-username:}")
    private String adminUsername;

    @Value("${users.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.info("No bootstrap administrator configured; skipping");
            return;
        }

        String username = adminUsername.trim();
        if (userRepository.findByUsername(username).isPresent()) {
            log.info("Bootstrap administrator {} already exists; leaving it untouched", username);
            return;
        }

        if (adminPassword.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Refusing to create bootstrap administrator {}: password is shorter than {} characters",
                    username, MIN_PASSWORD_LENGTH);
            return;
        }

        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(adminPassword))
                .email(username + "@nexgen.local")
                .role(ROLE_ADMIN)
                .enabled(true)
                .build());

        // The password itself is never logged.
        log.info("Created bootstrap administrator {} with {}", username, ROLE_ADMIN);
    }
}
