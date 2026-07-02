package com.message.mesh.service;

import com.message.mesh.domain.ChatMessage;
import com.message.mesh.domain.MessageReaction;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.enums.MessageStatus;
import com.message.mesh.repository.MessageReactionRepository;
import com.message.mesh.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageDtoAssembler")
class MessageDtoAssemblerTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private MessageReactionRepository reactionRepository;

    @InjectMocks
    private MessageDtoAssembler assembler;

    private ChatMessage message(UUID id, UUID parentId) {
        return ChatMessage.builder()
                .id(id)
                .conversationId(UUID.randomUUID())
                .senderUsername("alice")
                .seq(1L)
                .body("hello")
                .status(MessageStatus.SENT)
                .parentId(parentId)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("toDtos returns an empty list when given no messages")
    void toDtosEmptyForNoMessages() {
        assertThat(assembler.toDtos(List.of())).isEmpty();
    }

    @Test
    @DisplayName("toDto aggregates reactions grouped by emoji")
    void toDtoAggregatesReactions() {
        UUID msgId = UUID.randomUUID();
        ChatMessage msg = message(msgId, null);
        when(reactionRepository.findByMessageIdIn(anyCollection())).thenReturn(List.of(
                MessageReaction.builder().messageId(msgId).username("bob").emoji("\uD83D\uDC4D").build(),
                MessageReaction.builder().messageId(msgId).username("carol").emoji("\uD83D\uDC4D").build()));

        MessageDto dto = assembler.toDto(msg);

        assertThat(dto.reactions()).hasSize(1);
        MessageDto.ReactionSummary summary = dto.reactions().get(0);
        assertThat(summary.emoji()).isEqualTo("\uD83D\uDC4D");
        assertThat(summary.count()).isEqualTo(2);
        assertThat(summary.usernames()).containsExactlyInAnyOrder("bob", "carol");
    }

    @Test
    @DisplayName("toDto builds a parent preview for a threaded reply")
    void toDtoBuildsParentPreview() {
        UUID parentId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        ChatMessage reply = message(msgId, parentId);
        ChatMessage parent = message(parentId, null);
        parent.setBody("original text");
        parent.setSenderUsername("bob");
        when(reactionRepository.findByMessageIdIn(anyCollection())).thenReturn(List.of());
        when(messageRepository.findAllById(anyCollection())).thenReturn(List.of(parent));

        MessageDto dto = assembler.toDto(reply);

        assertThat(dto.parentPreview()).isNotNull();
        assertThat(dto.parentPreview().id()).isEqualTo(parentId);
        assertThat(dto.parentPreview().senderUsername()).isEqualTo("bob");
        assertThat(dto.parentPreview().body()).isEqualTo("original text");
    }

    @Test
    @DisplayName("toDto hides the body of a deleted parent in the preview")
    void toDtoHidesDeletedParentBody() {
        UUID parentId = UUID.randomUUID();
        ChatMessage reply = message(UUID.randomUUID(), parentId);
        ChatMessage parent = message(parentId, null);
        parent.setDeleted(true);
        parent.setBody("secret");
        when(reactionRepository.findByMessageIdIn(anyCollection())).thenReturn(List.of());
        when(messageRepository.findAllById(anyCollection())).thenReturn(List.of(parent));

        MessageDto dto = assembler.toDto(reply);

        assertThat(dto.parentPreview().body()).isEmpty();
    }

    @Test
    @DisplayName("toDto yields a null preview when the parent no longer exists")
    void toDtoNullPreviewWhenParentMissing() {
        UUID parentId = UUID.randomUUID();
        ChatMessage reply = message(UUID.randomUUID(), parentId);
        when(reactionRepository.findByMessageIdIn(anyCollection())).thenReturn(List.of());
        when(messageRepository.findAllById(anyCollection())).thenReturn(List.of());

        MessageDto dto = assembler.toDto(reply);

        assertThat(dto.parentPreview()).isNull();
    }

    @Test
    @DisplayName("toDto returns no reactions and no preview for a plain message")
    void toDtoPlainMessage() {
        ChatMessage msg = message(UUID.randomUUID(), null);
        when(reactionRepository.findByMessageIdIn(anyCollection())).thenReturn(List.of());

        MessageDto dto = assembler.toDto(msg);

        assertThat(dto.reactions()).isEmpty();
        assertThat(dto.parentPreview()).isNull();
    }
}
