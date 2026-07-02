package com.message.mesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Rename a group conversation. */
public record RenameConversationRequest(
        @NotBlank @Size(max = 128) String title
) {
}
