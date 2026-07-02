package com.message.mesh.service;

import com.message.mesh.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SequenceGenerator")
class SequenceGeneratorTest {

    @Mock
    private MessageRepository messageRepository;

    @Test
    @DisplayName("next seeds from the DB high-water mark and increments monotonically")
    void nextSeedsFromDbAndIncrements() {
        UUID conversationId = UUID.randomUUID();
        when(messageRepository.findMaxSeq(conversationId)).thenReturn(10L);
        SequenceGenerator generator = new SequenceGenerator(messageRepository);

        assertThat(generator.next(conversationId)).isEqualTo(11L);
        assertThat(generator.next(conversationId)).isEqualTo(12L);
        assertThat(generator.next(conversationId)).isEqualTo(13L);
    }

    @Test
    @DisplayName("next starts at 1 for a brand-new conversation")
    void nextStartsAtOneForNewConversation() {
        UUID conversationId = UUID.randomUUID();
        when(messageRepository.findMaxSeq(conversationId)).thenReturn(0L);
        SequenceGenerator generator = new SequenceGenerator(messageRepository);

        assertThat(generator.next(conversationId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("next maintains independent counters per conversation")
    void nextIsIndependentPerConversation() {
        UUID convA = UUID.randomUUID();
        UUID convB = UUID.randomUUID();
        when(messageRepository.findMaxSeq(convA)).thenReturn(5L);
        when(messageRepository.findMaxSeq(convB)).thenReturn(0L);
        SequenceGenerator generator = new SequenceGenerator(messageRepository);

        assertThat(generator.next(convA)).isEqualTo(6L);
        assertThat(generator.next(convB)).isEqualTo(1L);
        assertThat(generator.next(convA)).isEqualTo(7L);
    }
}
