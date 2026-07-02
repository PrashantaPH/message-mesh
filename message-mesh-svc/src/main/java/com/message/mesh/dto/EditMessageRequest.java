package com.message.mesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edit the body of an existing message (author only). */
public record EditMessageRequest(
        @NotBlank @Size(max = 4000) String body
) {
}
