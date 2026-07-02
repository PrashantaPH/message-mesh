package com.message.mesh.dto;

import java.util.UUID;

public record TypingNotification(UUID conversationId, String username) {
}
