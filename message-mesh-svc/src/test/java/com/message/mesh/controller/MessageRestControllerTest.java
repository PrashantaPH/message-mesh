package com.message.mesh.controller;

import com.message.mesh.dto.EditMessageRequest;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.ReactionRequest;
import com.message.mesh.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageRestController")
class MessageRestControllerTest {

    @Mock
    private MessageService messageService;
    @Mock
    private Principal principal;

    @InjectMocks
    private MessageRestController controller;

    private final UUID messageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(principal.getName()).thenReturn("alice");
    }

    @Test
    @DisplayName("edit delegates to editMessage")
    void editDelegates() {
        MessageDto expected = mock(MessageDto.class);
        when(messageService.editMessage("alice", messageId, "updated")).thenReturn(expected);

        assertThat(controller.edit(messageId, new EditMessageRequest("updated"), principal))
                .isSameAs(expected);
    }

    @Test
    @DisplayName("delete delegates to deleteMessage")
    void deleteDelegates() {
        MessageDto expected = mock(MessageDto.class);
        when(messageService.deleteMessage("alice", messageId)).thenReturn(expected);

        assertThat(controller.delete(messageId, principal)).isSameAs(expected);
    }

    @Test
    @DisplayName("addReaction delegates to addReaction")
    void addReactionDelegates() {
        MessageDto expected = mock(MessageDto.class);
        when(messageService.addReaction("alice", messageId, "\uD83D\uDC4D")).thenReturn(expected);

        assertThat(controller.addReaction(messageId, new ReactionRequest("\uD83D\uDC4D"), principal))
                .isSameAs(expected);
    }

    @Test
    @DisplayName("removeReaction delegates to removeReaction")
    void removeReactionDelegates() {
        MessageDto expected = mock(MessageDto.class);
        when(messageService.removeReaction("alice", messageId, "\uD83D\uDC4D")).thenReturn(expected);

        assertThat(controller.removeReaction(messageId, "\uD83D\uDC4D", principal)).isSameAs(expected);
    }
}
