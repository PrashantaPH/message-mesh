package com.message.mesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service update of the caller's own display name. */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 128) String displayName
) {
}
