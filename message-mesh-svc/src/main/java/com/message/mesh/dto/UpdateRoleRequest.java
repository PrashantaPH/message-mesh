package com.message.mesh.dto;

import com.message.mesh.enums.UserRole;
import jakarta.validation.constraints.NotNull;

/**
 * Admin request to change a user's global role.
 */
public record UpdateRoleRequest(
        @NotNull UserRole role
) {
}
