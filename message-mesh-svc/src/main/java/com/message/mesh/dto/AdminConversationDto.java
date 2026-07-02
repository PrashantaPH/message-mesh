package com.message.mesh.dto;

import com.message.mesh.domain.Conversation;
import com.message.mesh.enums.ConversationType;

import java.time.Instant;
import java.util.UUID;

/** Administrative projection of a conversation for the admin console. */
public record AdminConversationDto(
        UUID id,
        ConversationType type,
        String title,
        int memberCount,
        long messageCount,
        Instant createdAt,
        boolean deleted
) {

    public static AdminConversationDto from(Conversation c, int memberCount, long messageCount) {
        return new AdminConversationDto(
                c.getId(),
                c.getType(),
                c.getTitle(),
                memberCount,
                messageCount,
                c.getCreatedAt(),
                c.isDeleted());
    }
}
