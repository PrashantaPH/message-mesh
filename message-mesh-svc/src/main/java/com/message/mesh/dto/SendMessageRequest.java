package com.message.mesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Client -> server payload to send a message.
 *
 * @param clientTempId client-generated id used for optimistic UI reconciliation
 * @param parentId     optional id of the message being replied to (threading)
 */
public record SendMessageRequest(
        @NotNull UUID conversationId,
        @NotBlank @Size(max = 4000) String body,
        String clientTempId,
        UUID parentId
) {
}
