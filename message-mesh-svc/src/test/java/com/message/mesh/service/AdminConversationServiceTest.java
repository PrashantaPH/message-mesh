package com.message.mesh.service;

import com.message.mesh.domain.ChatMessage;
import com.message.mesh.domain.Conversation;
import com.message.mesh.dto.AdminConversationDto;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.enums.MessageStatus;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.repository.ConversationRepository;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.MessageReactionRepository;
import com.message.mesh.repository.MessageRepository;
import com.message.mesh.service.relay.MessageRelay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminConversationService")
class AdminConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageReactionRepository reactionRepository;
    @Mock
    private MessageDtoAssembler messageDtoAssembler;
    @Mock
    private MessageRelay relay;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminConversationService service;

    private Conversation conversation(UUID id) {
        return Conversation.builder().id(id).type(ConversationType.GROUP)
                .title("Team").createdAt(java.time.Instant.now()).build();
    }

    @Test
    @DisplayName("listConversations maps the page with member and message counts")
    void listConversationsMapsPage() {
        UUID convId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Conversation> page = new PageImpl<>(List.of(conversation(convId)), pageable, 1);
        when(conversationRepository.search(eq("team"), eq(ConversationType.GROUP), eq(false), eq(pageable)))
                .thenReturn(page);
        when(membershipRepository.countByConversationId(convId)).thenReturn(4L);
        when(messageRepository.countByConversationId(convId)).thenReturn(12L);

        PagedResponse<AdminConversationDto> result =
                service.listConversations("team", ConversationType.GROUP, false, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).memberCount()).isEqualTo(4);
        assertThat(result.content().get(0).messageCount()).isEqualTo(12L);
    }

    @Test
    @DisplayName("softDelete flags the conversation as deleted and audits it")
    void softDeleteFlagsConversation() {
        UUID convId = UUID.randomUUID();
        Conversation conv = conversation(convId);
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));

        AdminConversationDto dto = service.softDelete(convId, "root");

        assertThat(conv.isDeleted()).isTrue();
        assertThat(dto.deleted()).isTrue();
        verify(conversationRepository).save(conv);
        verify(auditService).record(eq("root"), eq("ADMIN_CONVERSATION_DELETED"), eq("conversation"),
                anyString(), any());
    }

    @Test
    @DisplayName("softDelete throws when the conversation is missing")
    void softDeleteThrowsWhenMissing() {
        UUID convId = UUID.randomUUID();
        when(conversationRepository.findById(convId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(convId, "root"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("restore clears the deleted flag")
    void restoreClearsFlag() {
        UUID convId = UUID.randomUUID();
        Conversation conv = conversation(convId);
        conv.setDeleted(true);
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));

        AdminConversationDto dto = service.restore(convId, "root");

        assertThat(conv.isDeleted()).isFalse();
        assertThat(dto.deleted()).isFalse();
        verify(auditService).record(eq("root"), eq("ADMIN_CONVERSATION_RESTORED"), eq("conversation"),
                anyString(), any());
    }

    @Test
    @DisplayName("listMessages returns a page for an existing conversation")
    void listMessagesReturnsPage() {
        UUID convId = UUID.randomUUID();
        when(conversationRepository.existsById(convId)).thenReturn(true);
        ChatMessage msg = ChatMessage.builder().id(UUID.randomUUID()).conversationId(convId)
                .senderUsername("bob").seq(1L).body("hi").status(MessageStatus.SENT).build();
        Pageable pageable = PageRequest.of(0, 50);
        Page<ChatMessage> page = new PageImpl<>(List.of(msg), pageable, 1);
        when(messageRepository.findByConversationIdOrderBySeqDesc(convId, pageable)).thenReturn(page);
        when(messageDtoAssembler.toDto(msg)).thenReturn(MessageDto.from(msg));

        PagedResponse<MessageDto> result = service.listMessages(convId, pageable);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("listMessages throws for an unknown conversation")
    void listMessagesThrowsWhenMissing() {
        UUID convId = UUID.randomUUID();
        when(conversationRepository.existsById(convId)).thenReturn(false);

        assertThatThrownBy(() -> service.listMessages(convId, PageRequest.of(0, 50)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteMessage soft-deletes, clears reactions, audits and re-broadcasts")
    void deleteMessageSoftDeletes() {
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(msgId).conversationId(convId)
                .senderUsername("bob").body("secret").build();
        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
        when(messageDtoAssembler.toDto(msg)).thenReturn(MessageDto.from(msg));

        MessageDto dto = service.deleteMessage(convId, msgId, "root");

        assertThat(msg.isDeleted()).isTrue();
        assertThat(msg.getBody()).isEmpty();
        assertThat(dto).isNotNull();
        verify(reactionRepository).deleteByMessageId(msgId);
        verify(relay).relay(eq(convId), any(MessageDto.class));
        verify(auditService).record(eq("root"), eq("ADMIN_MESSAGE_DELETED"), eq("message"),
                anyString(), anyString());
    }

    @Test
    @DisplayName("deleteMessage rejects a message that belongs to another conversation")
    void deleteMessageRejectsWrongConversation() {
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        ChatMessage msg = ChatMessage.builder().id(msgId).conversationId(UUID.randomUUID())
                .senderUsername("bob").body("hi").build();
        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> service.deleteMessage(convId, msgId, "root"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reactionRepository, never()).deleteByMessageId(any());
    }

    @Test
    @DisplayName("deleteMessage throws when the message does not exist")
    void deleteMessageThrowsWhenMissing() {
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        when(messageRepository.findById(msgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteMessage(convId, msgId, "root"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
