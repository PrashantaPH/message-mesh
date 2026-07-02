package com.message.mesh.dto;

import com.message.mesh.enums.ConversationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Create a DIRECT (2 members) or GROUP conversation.
 *
 * @param memberUsernames usernames to add besides the creator
 */
public record CreateConversationRequest(
        @NotNull ConversationType type,
        @Size(max = 128) String title,
        @NotEmpty List<String> memberUsernames
) {
}
