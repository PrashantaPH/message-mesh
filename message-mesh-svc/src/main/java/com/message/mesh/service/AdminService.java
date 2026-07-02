package com.message.mesh.service;

import com.message.mesh.domain.Conversation;
import com.message.mesh.domain.Membership;
import com.message.mesh.domain.User;
import com.message.mesh.dto.AdminConversationDto;
import com.message.mesh.dto.AdminStatsDto;
import com.message.mesh.dto.AdminUserDetailDto;
import com.message.mesh.dto.AdminUserDto;
import com.message.mesh.dto.CreateUserRequest;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.enums.UserRole;
import com.message.mesh.exception.BadRequestException;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.repository.AuditEventRepository;
import com.message.mesh.repository.ConversationRepository;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.MessageRepository;
import com.message.mesh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Administrative user-management operations. All methods assume the caller has
 * already been authorized as an administrator at the web layer
 * ({@code @PreAuthorize("hasRole('ADMIN')")}); the guardrails here protect
 * against business-rule violations such as removing the last remaining admin
 * or an administrator locking themselves out.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AuditEventRepository auditEventRepository;
    private final PresenceService presenceService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserDto> listUsers(String query, UserRole role, Boolean active, Pageable pageable) {
        String needle = query == null || query.trim().isEmpty() ? null : query.trim();
        Page<User> page = userRepository.search(needle, role, active, pageable);
        Set<String> online = presenceService.onlineUsernames();
        List<AdminUserDto> content = page.getContent().stream()
                .map(u -> toDto(u, online))
                .toList();
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public AdminUserDto updateRole(UUID id, UserRole role, String actingUsername) {
        User user = getUser(id);
        boolean demoting = user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN;
        if (demoting && user.getUsername().equals(actingUsername)) {
            throw new BadRequestException("You cannot revoke your own admin role");
        }
        if (demoting && adminCount() <= 1) {
            throw new BadRequestException("Cannot demote the last remaining administrator");
        }
        user.setRole(role);
        bumpTokenVersion(user);
        User saved = userRepository.save(user);
        auditService.record(actingUsername, "ADMIN_ROLE_CHANGED", "user",
                saved.getId().toString(), "role=" + role);
        log.info("Admin '{}' changed role of '{}' to {}", actingUsername, saved.getUsername(), role);
        return toDto(saved, presenceService.onlineUsernames());
    }

    @Transactional
    public AdminUserDto updateStatus(UUID id, boolean active, String actingUsername) {
        User user = getUser(id);
        if (!active && user.getUsername().equals(actingUsername)) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        if (!active && user.getRole() == UserRole.ADMIN && user.isActive() && activeAdminCount() <= 1) {
            throw new BadRequestException("Cannot deactivate the last remaining administrator");
        }
        user.setActive(active);
        if (!active) {
            bumpTokenVersion(user);
        }
        User saved = userRepository.save(user);
        auditService.record(actingUsername, active ? "ADMIN_USER_ACTIVATED" : "ADMIN_USER_DEACTIVATED",
                "user", saved.getId().toString(), null);
        log.info("Admin '{}' set active={} for '{}'", actingUsername, active, saved.getUsername());
        return toDto(saved, presenceService.onlineUsernames());
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword, String actingUsername) {
        User user = getUser(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        bumpTokenVersion(user);
        userRepository.save(user);
        auditService.record(actingUsername, "ADMIN_PASSWORD_RESET", "user",
                user.getId().toString(), null);
        log.info("Admin '{}' reset the password of '{}'", actingUsername, user.getUsername());
    }

    @Transactional
    public void deleteUser(UUID id, String actingUsername) {
        User user = getUser(id);
        if (user.getUsername().equals(actingUsername)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        if (user.getRole() == UserRole.ADMIN && adminCount() <= 1) {
            throw new BadRequestException("Cannot delete the last remaining administrator");
        }
        membershipRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
        auditService.record(actingUsername, "ADMIN_USER_DELETED", "user",
                user.getId().toString(), "username=" + user.getUsername());
        log.info("Admin '{}' deleted user '{}'", actingUsername, user.getUsername());
    }

    @Transactional
    public AdminUserDto createUser(CreateUserRequest request, String actingUsername) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username '" + username + "' is already taken");
        }
        User user = User.builder()
                .username(username)
                .displayName(request.displayName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build();
        User saved = userRepository.save(user);
        auditService.record(actingUsername, "ADMIN_USER_CREATED", "user",
                saved.getId().toString(), "username=" + saved.getUsername() + " role=" + saved.getRole());
        log.info("Admin '{}' created user '{}' with role {}", actingUsername, saved.getUsername(), saved.getRole());
        return toDto(saved, presenceService.onlineUsernames());
    }

    /**
     * Forces the user to re-authenticate everywhere by advancing the token
     * version, invalidating all outstanding JWTs (an admin "force logout").
     */
    @Transactional
    public AdminUserDto revokeSessions(UUID id, String actingUsername) {
        User user = getUser(id);
        bumpTokenVersion(user);
        User saved = userRepository.save(user);
        auditService.record(actingUsername, "ADMIN_SESSIONS_REVOKED", "user",
                saved.getId().toString(), null);
        log.info("Admin '{}' revoked sessions for '{}'", actingUsername, saved.getUsername());
        return toDto(saved, presenceService.onlineUsernames());
    }

    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserDetail(UUID id) {
        User user = getUser(id);
        Set<String> online = presenceService.onlineUsernames();
        List<AdminConversationDto> conversations = membershipRepository.findByUserId(id).stream()
                .map(Membership::getConversationId)
                .distinct()
                .map(conversationRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .sorted(Comparator.comparing(Conversation::getCreatedAt).reversed())
                .map(this::toConversationDto)
                .toList();
        return new AdminUserDetailDto(toDto(user, online), conversations);
    }

    @Transactional(readOnly = true)
    public AdminStatsDto stats() {
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActive(true);
        return new AdminStatsDto(
                totalUsers,
                activeUsers,
                totalUsers - activeUsers,
                userRepository.countByRole(UserRole.ADMIN),
                presenceService.onlineUsernames().size(),
                userRepository.countByCreatedAtAfter(weekAgo),
                conversationRepository.count(),
                conversationRepository.countByType(ConversationType.GROUP),
                conversationRepository.countByType(ConversationType.DIRECT),
                conversationRepository.countByDeleted(true),
                messageRepository.count(),
                messageRepository.countByCreatedAtAfter(weekAgo),
                auditEventRepository.count());
    }

    private AdminConversationDto toConversationDto(Conversation conversation) {
        int memberCount = (int) membershipRepository.countByConversationId(conversation.getId());
        long messageCount = messageRepository.countByConversationId(conversation.getId());
        return AdminConversationDto.from(conversation, memberCount, messageCount);
    }

    /**
     * Invalidates every existing JWT for the user by advancing the token version,
     * forcing re-authentication (used on role change, deactivation, password reset).
     */
    private void bumpTokenVersion(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    private AdminUserDto toDto(User user, Set<String> online) {
        return AdminUserDto.from(
                user,
                online.contains(user.getUsername()),
                membershipRepository.countByUserId(user.getId()));
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private long adminCount() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .count();
    }

    private long activeAdminCount() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN && u.isActive())
                .count();
    }
}
