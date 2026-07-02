package com.message.mesh.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Admin request to activate ({@code true}) or deactivate ({@code false}) a user account.
 */
public record UpdateStatusRequest(
        @NotNull Boolean active
) {
}
