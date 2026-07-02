package com.message.mesh.service;

import com.message.mesh.constant.AppConstants;
import com.message.mesh.domain.ChatMessage;
import com.message.mesh.domain.Conversation;
import com.message.mesh.domain.Membership;
import com.message.mesh.domain.MessageReaction;
import com.message.mesh.domain.User;
import com.message.mesh.dto.AckDto;
import com.message.mesh.dto.AckRequest;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.SendMessageRequest;
import com.message.mesh.dto.TypingEvent;
import com.message.mesh.dto.TypingNotification;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.enums.MembershipRole;
import com.message.mesh.enums.MessageStatus;
import com.message.mesh.event.MessageCreatedEvent;
import com.message.mesh.exception.BadRequestException;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.logging.MdcKeys;
import com.message.mesh.repository.ConversationRepository;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.MessageReactionRepository;
import com.message.mesh.repository.MessageRepository;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.service.relay.MessageRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Core message handling: persistence, sequencing, real-time fan-out, acks,
 * typing indicators, plus edit/soft-delete/threading and emoji reactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    /** Fixed reaction palette accepted by the API. */
    private static final Set<String> ALLOWED_EMOJIS =
            Set.of("\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDE4F");

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ConversationRepository conversationRepository;
    private final MessageReactionRepository reactionRepository;
    private final MessageDtoAssembler messageDtoAssembler;
    private final MessageRelay relay;
    private final ApplicationEventPublisher events;
    private final SequenceGenerator sequenceGenerator;

    @Transactional
    public void handleSend(String senderUsername, SendMessageRequest req) {
        MDC.put(MdcKeys.CONVERSATION_ID, String.valueOf(req.conversationId()));
        try {
            assertMembership(senderUsername, req.conversationId());
            validateParent(req.conversationId(), req.parentId());

            long nextSeq = sequenceGenerator.next(req.conversationId());
            ChatMessage msg = ChatMessage.builder()
                    .conversationId(req.conversationId())
                    .senderUsername(senderUsername)
                    .seq(nextSeq)
                    .body(req.body())
                    .status(MessageStatus.SENT)
                    .parentId(req.parentId())
                    .createdAt(Instant.now())
                    .build();

            messageRepository.save(msg);
            log.info("Persisted message seq={} from '{}'", nextSeq, senderUsername);

            MessageDto enriched = messageDtoAssembler.toDto(msg);
            MessageDto dto = MessageDto.of(msg, req.clientTempId(),
                    enriched.parentPreview(), enriched.reactions());
            relay.relay(req.conversationId(), dto);
            events.publishEvent(new MessageCreatedEvent(dto));
        } finally {
            MDC.remove(MdcKeys.CONVERSATION_ID);
        }
    }

    @Transactional
    public void handleAck(String username, AckRequest req) {
        int updated = messageRepository.updateStatus(req.messageId(), MessageStatus.DELIVERED);
        if (updated == 0) {
            throw new ResourceNotFoundException("Message not found: " + req.messageId());
        }
        relay.relayToUser(username, AppConstants.QUEUE_ACK,
                new AckDto(req.messageId(), MessageStatus.DELIVERED));
    }

    public void handleTyping(String username, TypingEvent ev) {
        relay.broadcast(AppConstants.topicTyping(ev.conversationId()),
                new TypingNotification(ev.conversationId(), username));
    }

    // ------------------------------------------------------------------
    // Edit / soft-delete / reactions
    // ------------------------------------------------------------------

    /** Edit a message body (author only). Re-broadcasts the updated message. */
    @Transactional
    public MessageDto editMessage(String username, UUID messageId, String body) {
        ChatMessage msg = requireMessage(messageId);
        if (!msg.getSenderUsername().equals(username)) {
            throw new BadRequestException("Only the author can edit this message");
        }
        if (msg.isDeleted()) {
            throw new BadRequestException("Cannot edit a deleted message");
        }
        msg.setBody(body);
        msg.setEditedAt(Instant.now());
        messageRepository.save(msg);
        return rebroadcast(msg);
    }

    /** Soft-delete a message (author or a group admin). Re-broadcasts the tombstone. */
    @Transactional
    public MessageDto deleteMessage(String username, UUID messageId) {
        ChatMessage msg = requireMessage(messageId);
        boolean isAuthor = msg.getSenderUsername().equals(username);
        if (!isAuthor && !isGroupAdmin(username, msg.getConversationId())) {
            throw new BadRequestException("Only the author or a group admin can delete this message");
        }
        if (!msg.isDeleted()) {
            msg.setDeleted(true);
            msg.setBody("");
            messageRepository.save(msg);
            reactionRepository.deleteByMessageId(messageId);
        }
        return rebroadcast(msg);
    }

    /** Add an emoji reaction (idempotent per user+emoji). Re-broadcasts the message. */
    @Transactional
    public MessageDto addReaction(String username, UUID messageId, String emoji) {
        if (!ALLOWED_EMOJIS.contains(emoji)) {
            throw new BadRequestException("Unsupported reaction emoji");
        }
        ChatMessage msg = requireMessage(messageId);
        assertMembership(username, msg.getConversationId());
        if (reactionRepository.findByMessageIdAndUsernameAndEmoji(messageId, username, emoji).isEmpty()) {
            reactionRepository.save(MessageReaction.builder()
                    .messageId(messageId)
                    .username(username)
                    .emoji(emoji)
                    .build());
        }
        return rebroadcast(msg);
    }

    /** Remove the caller's emoji reaction. Re-broadcasts the message. */
    @Transactional
    public MessageDto removeReaction(String username, UUID messageId, String emoji) {
        ChatMessage msg = requireMessage(messageId);
        assertMembership(username, msg.getConversationId());
        reactionRepository.deleteByMessageIdAndUsernameAndEmoji(messageId, username, emoji);
        return rebroadcast(msg);
    }

    private MessageDto rebroadcast(ChatMessage msg) {
        MessageDto dto = messageDtoAssembler.toDto(msg);
        relay.relay(msg.getConversationId(), dto);
        return dto;
    }

    private void validateParent(UUID conversationId, UUID parentId) {
        if (parentId == null) {
            return;
        }
        ChatMessage parent = messageRepository.findById(parentId)
                .orElseThrow(() -> new BadRequestException("Replied-to message not found"));
        if (!parent.getConversationId().equals(conversationId)) {
            throw new BadRequestException("Replied-to message belongs to a different conversation");
        }
    }

    private boolean isGroupAdmin(String username, UUID conversationId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || conversation.getType() != ConversationType.GROUP) {
            return false;
        }
        return membershipRepository.findByUserIdAndConversationId(user.getId(), conversationId)
                .map(Membership::getRole)
                .filter(role -> role == MembershipRole.ADMIN)
                .isPresent();
    }

    private ChatMessage requireMessage(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
    }

    private void assertMembership(String username, UUID conversationId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        if (!membershipRepository.existsByUserIdAndConversationId(user.getId(), conversationId)) {
            throw new BadRequestException("User is not a member of conversation " + conversationId);
        }
    }
}
