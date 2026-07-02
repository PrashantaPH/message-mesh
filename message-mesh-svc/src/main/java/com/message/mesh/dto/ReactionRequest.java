package com.message.mesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Add a single emoji reaction to a message. */
public record ReactionRequest(
        @NotBlank @Size(max = 16) String emoji
) {
}
