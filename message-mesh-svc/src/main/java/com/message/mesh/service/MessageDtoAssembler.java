package com.message.mesh.service;

import com.message.mesh.domain.ChatMessage;
import com.message.mesh.domain.MessageReaction;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.repository.MessageReactionRepository;
import com.message.mesh.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builds enriched {@link MessageDto}s (reactions + reply previews) from persisted
 * {@link ChatMessage} rows. Batches its lookups to avoid the N+1 problem when a
 * whole page of history is rendered, and offers a single-message variant for
 * real-time re-broadcasts (edit/delete/reaction frames).
 */
@Component
@RequiredArgsConstructor
public class MessageDtoAssembler {

    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;

    /** Build a single enriched DTO (used for live re-broadcasts). */
    public MessageDto toDto(ChatMessage msg) {
        return toDtos(List.of(msg)).get(0);
    }

    /** Build enriched DTOs for a batch of messages using two batched queries. */
    public List<MessageDto> toDtos(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = messages.stream().map(ChatMessage::getId).toList();

        Map<UUID, Map<String, List<String>>> reactionsByMsg = new HashMap<>();
        for (MessageReaction reaction : reactionRepository.findByMessageIdIn(ids)) {
            reactionsByMsg
                    .computeIfAbsent(reaction.getMessageId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(reaction.getEmoji(), k -> new ArrayList<>())
                    .add(reaction.getUsername());
        }

        Set<UUID> parentIds = new LinkedHashSet<>();
        for (ChatMessage msg : messages) {
            if (msg.getParentId() != null) {
                parentIds.add(msg.getParentId());
            }
        }
        Map<UUID, ChatMessage> parents = new HashMap<>();
        if (!parentIds.isEmpty()) {
            for (ChatMessage parent : messageRepository.findAllById(parentIds)) {
                parents.put(parent.getId(), parent);
            }
        }

        List<MessageDto> result = new ArrayList<>(messages.size());
        for (ChatMessage msg : messages) {
            result.add(MessageDto.of(msg, null,
                    parentPreview(msg, parents),
                    toReactionSummaries(reactionsByMsg.get(msg.getId()))));
        }
        return result;
    }

    private MessageDto.ParentPreview parentPreview(ChatMessage msg, Map<UUID, ChatMessage> parents) {
        if (msg.getParentId() == null) {
            return null;
        }
        ChatMessage parent = parents.get(msg.getParentId());
        if (parent == null) {
            return null;
        }
        String preview = parent.isDeleted() ? "" : parent.getBody();
        return new MessageDto.ParentPreview(parent.getId(), parent.getSenderUsername(), preview);
    }

    private List<MessageDto.ReactionSummary> toReactionSummaries(Map<String, List<String>> byEmoji) {
        if (byEmoji == null || byEmoji.isEmpty()) {
            return List.of();
        }
        List<MessageDto.ReactionSummary> summaries = new ArrayList<>(byEmoji.size());
        byEmoji.forEach((emoji, users) ->
                summaries.add(new MessageDto.ReactionSummary(emoji, users.size(), List.copyOf(users))));
        return summaries;
    }
}
