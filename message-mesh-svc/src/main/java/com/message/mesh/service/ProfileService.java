package com.message.mesh.service;

import com.message.mesh.domain.User;
import com.message.mesh.dto.AuthResponse;
import com.message.mesh.dto.UserDto;
import com.message.mesh.enums.UserRole;
import com.message.mesh.exception.BadRequestException;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service account operations for the currently authenticated user. Unlike
 * {@link AdminService}, every method here acts on the caller's own account,
 * resolved from the JWT principal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    @Transactional
    public UserDto updateProfile(String username, String displayName) {
        User user = requireUser(username);
        user.setDisplayName(displayName);
        userRepository.save(user);
        auditService.record(username, "PROFILE_UPDATED", "user", user.getId().toString(),
                "displayName=" + displayName);
        return UserDto.from(user);
    }

    /**
     * Verifies the current password, applies the new one, and re-issues a JWT.
     * Advancing the token version invalidates other outstanding sessions while
     * the returned token keeps the caller signed in on this device.
     */
    @Transactional
    public AuthResponse changePassword(String username, String currentPassword, String newPassword) {
        User user = requireUser(username);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        auditService.record(username, "PASSWORD_CHANGED", "user", user.getId().toString(), null);

        String token = jwtUtil.generateToken(user.getUsername(), user.getTokenVersion());
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional
    public void deleteAccount(String username) {
        User user = requireUser(username);
        if (user.getRole() == UserRole.ADMIN && adminCount() <= 1) {
            throw new BadRequestException("Cannot delete the last remaining administrator");
        }
        membershipRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
        auditService.record(username, "ACCOUNT_DELETED", "user", user.getId().toString(), null);
        log.info("User '{}' deleted their own account", username);
    }

    private long adminCount() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .count();
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
