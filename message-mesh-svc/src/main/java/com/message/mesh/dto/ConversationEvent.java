package com.message.mesh.dto;

import java.util.UUID;

/**
 * Real-time envelope broadcast on {@code /topic/conv.{id}.meta} to notify clients
 * of conversation/membership changes so they can refresh their caches. Message
 * content changes travel on the main conversation topic instead.
 */
public record ConversationEvent(
        Type type,
        UUID conversationId,
        String actorUsername,
        String targetUsername,
        String title
) {

    public enum Type {
        RENAMED,
        MEMBER_ADDED,
        MEMBER_REMOVED,
        MEMBER_ROLE_CHANGED,
        CONVERSATION_DELETED
    }

    public static ConversationEvent of(Type type, UUID conversationId, String actorUsername) {
        return new ConversationEvent(type, conversationId, actorUsername, null, null);
    }
}
