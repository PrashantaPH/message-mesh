package com.message.mesh.service;

import com.message.mesh.domain.ChatMessage;
import com.message.mesh.domain.Conversation;
import com.message.mesh.dto.AdminConversationDto;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.repository.ConversationRepository;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.MessageReactionRepository;
import com.message.mesh.repository.MessageRepository;
import com.message.mesh.service.relay.MessageRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Administrative conversation oversight. All methods assume the caller was
 * authorized as an administrator at the web layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminConversationService {

    private final ConversationRepository conversationRepository;
    private final MembershipRepository membershipRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final MessageDtoAssembler messageDtoAssembler;
    private final MessageRelay relay;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<AdminConversationDto> listConversations(
            String query, ConversationType type, Boolean deleted, Pageable pageable) {
        String needle = query == null || query.trim().isEmpty() ? null : query.trim();
        Page<Conversation> page = conversationRepository.search(needle, type, deleted, pageable);
        List<AdminConversationDto> content = page.getContent().stream()
                .map(this::toDto)
                .toList();
        return new PagedResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public AdminConversationDto softDelete(UUID conversationId, String actingUsername) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        conversation.setDeleted(true);
        conversationRepository.save(conversation);
        auditService.record(actingUsername, "ADMIN_CONVERSATION_DELETED", "conversation",
                conversationId.toString(), null);
        log.info("Admin '{}' soft-deleted conversation {}", actingUsername, conversationId);
        return toDto(conversation);
    }

    @Transactional
    public AdminConversationDto restore(UUID conversationId, String actingUsername) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        conversation.setDeleted(false);
        conversationRepository.save(conversation);
        auditService.record(actingUsername, "ADMIN_CONVERSATION_RESTORED", "conversation",
                conversationId.toString(), null);
        log.info("Admin '{}' restored conversation {}", actingUsername, conversationId);
        return toDto(conversation);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MessageDto> listMessages(UUID conversationId, Pageable pageable) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new ResourceNotFoundException("Conversation not found: " + conversationId);
        }
        Page<ChatMessage> page = messageRepository.findByConversationIdOrderBySeqDesc(conversationId, pageable);
        List<MessageDto> content = page.getContent().stream()
                .map(messageDtoAssembler::toDto)
                .toList();
        return new PagedResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Soft-deletes an individual message for moderation and broadcasts the
     * tombstone so connected clients update in real time.
     */
    @Transactional
    public MessageDto deleteMessage(UUID conversationId, UUID messageId, String actingUsername) {
        ChatMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        if (!msg.getConversationId().equals(conversationId)) {
            throw new ResourceNotFoundException("Message not found in conversation: " + messageId);
        }
        if (!msg.isDeleted()) {
            msg.setDeleted(true);
            msg.setBody("");
            messageRepository.save(msg);
            reactionRepository.deleteByMessageId(messageId);
        }
        auditService.record(actingUsername, "ADMIN_MESSAGE_DELETED", "message",
                messageId.toString(), "conversation=" + conversationId);
        log.info("Admin '{}' deleted message {} in conversation {}", actingUsername, messageId, conversationId);
        MessageDto dto = messageDtoAssembler.toDto(msg);
        relay.relay(conversationId, dto);
        return dto;
    }

    private AdminConversationDto toDto(Conversation conversation) {
        int memberCount = (int) membershipRepository.countByConversationId(conversation.getId());
        long messageCount = messageRepository.countByConversationId(conversation.getId());
        return AdminConversationDto.from(conversation, memberCount, messageCount);
    }
}
