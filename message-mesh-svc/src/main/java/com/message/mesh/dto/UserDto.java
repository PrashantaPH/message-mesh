package com.message.mesh.dto;

import com.message.mesh.domain.User;
import com.message.mesh.enums.UserRole;

import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String displayName,
        UserRole role
) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
