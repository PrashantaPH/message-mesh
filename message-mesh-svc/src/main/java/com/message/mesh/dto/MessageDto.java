package com.message.mesh.dto;

import com.message.mesh.domain.ChatMessage;
import com.message.mesh.enums.MessageStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Server -> client message projection.
 *
 * @param clientTempId  echoed back so the sender can reconcile its optimistic entry
 * @param editedAt      set when the author edited the body; null otherwise
 * @param deleted       true when the message was soft-deleted (body is blank)
 * @param parentId      id of the replied-to message, if this is a threaded reply
 * @param parentPreview lightweight snapshot of the replied-to message for inline rendering
 * @param reactions     aggregated emoji reactions; {@code reactedByMe} is derived client-side
 */
public record MessageDto(
        UUID id,
        UUID conversationId,
        String senderUsername,
        long seq,
        String body,
        MessageStatus status,
        Instant createdAt,
        String clientTempId,
        Instant editedAt,
        boolean deleted,
        UUID parentId,
        ParentPreview parentPreview,
        List<ReactionSummary> reactions
) {

    /** Minimal snapshot of a replied-to message. */
    public record ParentPreview(UUID id, String senderUsername, String body) {
    }

    /** Aggregated reactions for a single emoji: the emoji, its count, and who reacted. */
    public record ReactionSummary(String emoji, int count, List<String> usernames) {
    }

    public static MessageDto from(ChatMessage msg) {
        return from(msg, null);
    }

    public static MessageDto from(ChatMessage msg, String clientTempId) {
        return of(msg, clientTempId, null, List.of());
    }

    public static MessageDto of(ChatMessage msg,
                                String clientTempId,
                                ParentPreview parentPreview,
                                List<ReactionSummary> reactions) {
        return new MessageDto(
                msg.getId(),
                msg.getConversationId(),
                msg.getSenderUsername(),
                msg.getSeq(),
                msg.getBody(),
                msg.getStatus(),
                msg.getCreatedAt(),
                clientTempId,
                msg.getEditedAt(),
                msg.isDeleted(),
                msg.getParentId(),
                parentPreview,
                reactions == null ? List.of() : reactions
        );
    }
}
