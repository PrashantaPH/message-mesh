package com.message.mesh.config;

import com.message.mesh.enums.UserRole;
import com.message.mesh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures at least one administrator exists on startup. If no user currently
 * has the {@link UserRole#ADMIN} role, the configured bootstrap username is
 * promoted (never created — a real account must already exist). This keeps the
 * admin flow reachable on databases that predate the role column without
 * hard-coding credentials.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${app.admin.bootstrap-username:}")
    private String bootstrapUsername;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() == UserRole.ADMIN);
        if (adminExists) {
            return;
        }
        if (bootstrapUsername == null || bootstrapUsername.isBlank()) {
            log.warn("No administrator exists and 'app.admin.bootstrap-username' is unset; "
                    + "the admin flow is inaccessible until a user is promoted manually.");
            return;
        }
        userRepository.findByUsername(bootstrapUsername).ifPresentOrElse(
                user -> {
                    user.setRole(UserRole.ADMIN);
                    userRepository.save(user);
                    log.info("Bootstrapped ADMIN role for existing user '{}'", bootstrapUsername);
                },
                () -> log.warn("Bootstrap admin username '{}' does not exist; no administrator was promoted. "
                        + "Register that user (or set 'app.admin.bootstrap-username') to enable the admin flow.",
                        bootstrapUsername)
        );
    }
}
