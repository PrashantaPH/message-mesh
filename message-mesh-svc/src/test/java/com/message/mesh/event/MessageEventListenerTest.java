package com.message.mesh.event;

import com.message.mesh.domain.ChatMessage;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.enums.MessageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("MessageEventListener")
class MessageEventListenerTest {

    private final MessageEventListener listener = new MessageEventListener();

    @Test
    @DisplayName("onMessageCreated handles the event without throwing")
    void onMessageCreatedDoesNotThrow() {
        ChatMessage msg = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(UUID.randomUUID())
                .senderUsername("alice")
                .seq(1L)
                .body("hello")
                .status(MessageStatus.SENT)
                .build();
        MessageCreatedEvent event = new MessageCreatedEvent(MessageDto.from(msg));

        assertThatCode(() -> listener.onMessageCreated(event)).doesNotThrowAnyException();
    }
}
