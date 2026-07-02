package com.message.mesh.dto;

import com.message.mesh.enums.ConversationType;

import java.util.UUID;

public record ConversationDto(
        UUID id,
        ConversationType type,
        String title,
        MessageDto lastMessage,
        long unreadCount,
        int memberCount,
        boolean muted,
        boolean archived
) {
}
