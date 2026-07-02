package com.message.mesh.service;

import com.message.mesh.constant.AppConstants;
import com.message.mesh.domain.ChatMessage;
import com.message.mesh.domain.Conversation;
import com.message.mesh.domain.Membership;
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
import com.message.mesh.repository.ConversationRepository;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.MessageReactionRepository;
import com.message.mesh.repository.MessageRepository;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.service.relay.MessageRelay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService")
class MessageServiceTest {

    private static final String THUMBS_UP = "\uD83D\uDC4D";

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageReactionRepository reactionRepository;
    @Mock
    private MessageDtoAssembler messageDtoAssembler;
    @Mock
    private MessageRelay relay;
    @Mock
    private ApplicationEventPublisher events;
    @Mock
    private SequenceGenerator sequenceGenerator;

    @InjectMocks
    private MessageService messageService;

    private UUID conversationId;
    private UUID userId;
    private User alice;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        alice = User.builder().id(userId).username("alice").passwordHash("x").displayName("Alice").build();
    }

    private MessageDto sampleDto(UUID msgId) {
        return new MessageDto(msgId, conversationId, "alice", 1L, "hello",
                MessageStatus.SENT, Instant.now(), null, null, false, null, null, java.util.List.of());
    }

    @Test
    @DisplayName("handleSend persists a sequenced message, relays it and publishes an event")
    void handleSendPersistsAndBroadcasts() {
        SendMessageRequest req = new SendMessageRequest(conversationId, "hello", "tmp-1", null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(membershipRepository.existsByUserIdAndConversationId(userId, conversationId)).thenReturn(true);
        when(sequenceGenerator.next(conversationId)).thenReturn(5L);
        when(messageDtoAssembler.toDto(any(ChatMessage.class)))
                .thenReturn(sampleDto(UUID.randomUUID()));

        messageService.handleSend("alice", req);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getSeq()).isEqualTo(5L);
        assertThat(captor.getValue().getSenderUsername()).isEqualTo("alice");
        assertThat(captor.getValue().getStatus()).isEqualTo(MessageStatus.SENT);

        verify(relay).relay(eq(conversationId), any(MessageDto.class));
        verify(events).publishEvent(any(MessageCreatedEvent.class));
    }

    @Test
    @DisplayName("handleSend rejects a non-member sender")
    void handleSendRejectsNonMember() {
        SendMessageRequest req = new SendMessageRequest(conversationId, "hello", null, null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(membershipRepository.existsByUserIdAndConversationId(userId, conversationId)).thenReturn(false);

        assertThatThrownBy(() -> messageService.handleSend("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a member");

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("handleSend throws when the sender does not exist")
    void handleSendThrowsForUnknownSender() {
        SendMessageRequest req = new SendMessageRequest(conversationId, "hello", null, null);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.handleSend("ghost", req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("handleSend validates the parent belongs to the same conversation")
    void handleSendRejectsForeignParent() {
        UUID parentId = UUID.randomUUID();
        SendMessageRequest req = new SendMessageRequest(conversationId, "hello", null, parentId);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(membershipRepository.existsByUserIdAndConversationId(userId, conversationId)).thenReturn(true);
        ChatMessage parent = ChatMessage.builder().id(parentId)
                .conversationId(UUID.randomUUID()).build();
        when(messageRepository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> messageService.handleSend("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("different conversation");
    }

    @Test
    @DisplayName("handleSend rejects a reply to a non-existent parent")
    void handleSendRejectsMissingParent() {
        UUID parentId = UUID.randomUUID();
        SendMessageRequest req = new SendMessageRequest(conversationId, "hello", null, parentId);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(membershipRepository.existsByUserIdAndConversationId(userId, conversationId)).thenReturn(true);
        when(messageRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.handleSend("alice", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Replied-to message not found");
    }

    @Test
    @DisplayName("handleAck flips the status to DELIVERED and notifies the sender")
    void handleAckUpdatesStatus() {
        UUID messageId = UUID.randomUUID();
        AckRequest req = new AckRequest(messageId);
        when(messageRepository.updateStatus(messageId, MessageStatus.DELIVERED)).thenReturn(1);

        messageService.handleAck("alice", req);

        verify(relay).relayToUser("alice", AppConstants.QUEUE_ACK,
                new AckDto(messageId, MessageStatus.DELIVERED));
    }

    @Test
    @DisplayName("handleAck throws when the message does not exist")
    void handleAckThrowsWhenNotFound() {
        UUID messageId = UUID.randomUUID();
        when(messageRepository.updateStatus(messageId, MessageStatus.DELIVERED)).thenReturn(0);

        assertThatThrownBy(() -> messageService.handleAck("alice", new AckRequest(messageId)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(relay, never()).relayToUser(any(), any(), any());
    }

    @Test
    @DisplayName("handleTyping broadcasts a typing notification to the conversation topic")
    void handleTypingBroadcasts() {
        messageService.handleTyping("alice", new TypingEvent(conversationId));

        verify(relay).broadcast(AppConstants.topicTyping(conversationId),
                new TypingNotification(conversationId, "alice"));
    }

    @Test
    @DisplayName("editMessage updates the body for the author and re-broadcasts")
    void editMessageSucceedsForAuthor() {
        UUID messageId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(messageId).conversationId(conversationId)
                .senderUsername("alice").body("old").status(MessageStatus.SENT).build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        when(messageDtoAssembler.toDto(msg)).thenReturn(sampleDto(messageId));

        messageService.editMessage("alice", messageId, "new body");

        assertThat(msg.getBody()).isEqualTo("new body");
        assertThat(msg.getEditedAt()).isNotNull();
        verify(messageRepository).save(msg);
        verify(relay).relay(eq(conversationId), any(MessageDto.class));
    }

    @Test
    @DisplayName("editMessage rejects a non-author")
    void editMessageRejectsNonAuthor() {
        UUID messageId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(messageId).conversationId(conversationId)
                .senderUsername("bob").body("old").build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> messageService.editMessage("alice", messageId, "new"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only the author");
    }

    @Test
    @DisplayName("editMessage rejects editing a deleted message")
    void editMessageRejectsDeleted() {
        UUID messageId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(messageId).conversationId(conversationId)
                .senderUsername("alice").body("").deleted(true).build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> messageService.editMessage("alice", messageId, "new"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deleted message");
    }

    @Test
    @DisplayName("deleteMessage soft-deletes for the author and clears reactions")
    void deleteMessageSucceedsForAuthor() {
        UUID messageId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(messageId).conversationId(conversationId)
                .senderUsername("alice").body("secret").build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        when(messageDtoAssembler.toDto(msg)).thenReturn(sampleDto(messageId));

        messageService.deleteMessage("alice", messageId);

        assertThat(msg.isDeleted()).isTrue();
        assertThat(msg.getBody()).isEmpty();
        verify(reactionRepository).deleteByMessageId(messageId);
        verify(relay).relay(eq(conversationId), any(MessageDto.class));
    }

    @Test
    @DisplayName("deleteMessage allows a group admin to delete another user's message")
    void deleteMessageAllowedForGroupAdmin() {
        UUID messageId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(messageId).conversationId(conversationId)
                .senderUsername("bob").body("hi").build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Conversation group = Conversation.builder().id(conversationId).type(ConversationType.GROUP).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(group));
        when(membershipRepository.findByUserIdAndConversationId(userId, conversationId))
                .thenReturn(Optional.of(Membership.builder().role(MembershipRole.ADMIN).build()));
        when(messageDtoAssembler.toDto(msg)).thenReturn(sampleDto(messageId));

        messageService.deleteMessage("alice", messageId);

        assertThat(msg.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("deleteMessage rejects a non-author who is not a group admin")
    void deleteMessageRejectsUnauthorized() {
        UUID messageId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(messageId).conversationId(conversationId)
                .senderUsername("bob").body("hi").build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        Conversation direct = Conversation.builder().id(conversationId).type(ConversationType.DIRECT).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(direct));

        assertThatThrownBy(() -> messageService.deleteMessage("alice", messageId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("author or a group admin");
    }

    @Test
    @DisplayName("addReaction persists a new reaction from the fixed palette")
    void addReactionPersistsNew() {
        UUID messageId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(messageId).conversationId(conversationId)
                .senderUsername("bob").body("hi").build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(membershipRepository.existsByUserIdAndConversationId(userId, conversationId)).thenReturn(true);
        when(reactionRepository.findByMessageIdAndUsernameAndEmoji(messageId, "alice", THUMBS_UP))
                .thenReturn(Optional.empty());
        when(messageDtoAssembler.toDto(msg)).thenReturn(sampleDto(messageId));

        messageService.addReaction("alice", messageId, THUMBS_UP);

        verify(reactionRepository).save(any());
        verify(relay).relay(eq(conversationId), any(MessageDto.class));
    }

    @Test
    @DisplayName("addReaction is idempotent for an existing user+emoji pair")
    void addReactionIdempotent() {
        UUID messageId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(messageId).conversationId(conversationId)
                .senderUsername("bob").body("hi").build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(membershipRepository.existsByUserIdAndConversationId(userId, conversationId)).thenReturn(true);
        when(reactionRepository.findByMessageIdAndUsernameAndEmoji(messageId, "alice", THUMBS_UP))
                .thenReturn(Optional.of(com.message.mesh.domain.MessageReaction.builder().build()));
        when(messageDtoAssembler.toDto(msg)).thenReturn(sampleDto(messageId));

        messageService.addReaction("alice", messageId, THUMBS_UP);

        verify(reactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("addReaction rejects an emoji outside the allowed palette")
    void addReactionRejectsUnsupportedEmoji() {
        assertThatThrownBy(() -> messageService.addReaction("alice", UUID.randomUUID(), "\uD83D\uDCA9"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported reaction");

        verify(messageRepository, never()).findById(any());
    }

    @Test
    @DisplayName("removeReaction deletes the caller's reaction and re-broadcasts")
    void removeReactionDeletes() {
        UUID messageId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(messageId).conversationId(conversationId)
                .senderUsername("bob").body("hi").build();
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(msg));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(membershipRepository.existsByUserIdAndConversationId(userId, conversationId)).thenReturn(true);
        when(messageDtoAssembler.toDto(msg)).thenReturn(sampleDto(messageId));

        messageService.removeReaction("alice", messageId, THUMBS_UP);

        verify(reactionRepository).deleteByMessageIdAndUsernameAndEmoji(messageId, "alice", THUMBS_UP);
        verify(relay).relay(eq(conversationId), any(MessageDto.class));
    }
}
