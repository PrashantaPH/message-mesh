package com.message.mesh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A single emoji reaction placed by a user on a message. The unique constraint
 * ensures a user can apply a given emoji to a message at most once.
 */
@Entity
@Table(name = "message_reactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_reaction_msg_user_emoji",
                columnNames = {"messageId", "username", "emoji"}),
        indexes = @Index(name = "idx_reaction_message", columnList = "messageId"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReaction {

    @Id
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID messageId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 16)
    private String emoji;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
