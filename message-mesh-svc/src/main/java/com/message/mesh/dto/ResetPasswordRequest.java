package com.message.mesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin request to set a new password for a user. The policy mirrors registration.
 */
public record ResetPasswordRequest(
        @NotBlank @Size(min = 6, max = 100) String newPassword
) {
}
