package com.message.mesh.dto;

import com.message.mesh.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin request to provision a new user account. Mirrors the registration
 * policy but also lets the admin assign the initial role.
 */
public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 128) String displayName,
        @NotNull UserRole role
) {
}
