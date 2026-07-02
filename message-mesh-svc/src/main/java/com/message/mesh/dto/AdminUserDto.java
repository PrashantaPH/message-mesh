package com.message.mesh.dto;

import com.message.mesh.domain.User;
import com.message.mesh.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Full user projection exposed only to administrators via the admin APIs.
 * Never includes the password hash.
 */
public record AdminUserDto(
        UUID id,
        String username,
        String displayName,
        UserRole role,
        boolean active,
        Instant createdAt,
        boolean online,
        long conversationCount
) {

    public static AdminUserDto from(User user, boolean online, long conversationCount) {
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                online,
                conversationCount
        );
    }
}
