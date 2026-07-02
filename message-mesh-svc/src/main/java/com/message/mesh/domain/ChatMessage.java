package com.message.mesh.domain;

import com.message.mesh.enums.MessageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages",
        uniqueConstraints = @UniqueConstraint(name = "uk_message_conv_seq",
                columnNames = {"conversationId", "seq"}),
        indexes = {
                @Index(name = "idx_message_conversation_seq", columnList = "conversationId, seq")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID conversationId;

    @Column(nullable = false, length = 64)
    private String senderUsername;

    /** Monotonic, per-conversation ordering sequence. */
    @Column(nullable = false)
    private long seq;

    @Column(nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    /** Set when the message body has been edited by its author; null otherwise. */
    private Instant editedAt;

    /** Soft-delete flag: a deleted message is retained for ordering but its body is cleared. */
    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean deleted = false;

    /** Optional id of the message this one replies to (threading). */
    private UUID parentId;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
