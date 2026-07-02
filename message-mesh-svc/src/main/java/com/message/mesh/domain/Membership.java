package com.message.mesh.domain;

import com.message.mesh.enums.MembershipRole;
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
@Table(name = "memberships",
        uniqueConstraints = @UniqueConstraint(name = "uk_membership_user_conv",
                columnNames = {"userId", "conversationId"}),
        indexes = {
                @Index(name = "idx_membership_user", columnList = "userId"),
                @Index(name = "idx_membership_conversation", columnList = "conversationId")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Membership {

    @Id
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private MembershipRole role = MembershipRole.MEMBER;

    @Column(nullable = false)
    @Builder.Default
    private long lastReadSeq = 0L;

    /** Per-user preference: suppress notifications for this conversation. */
    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean muted = false;

    /** Per-user preference: hide this conversation from the caller's list. */
    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean archived = false;

    @Column(nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Builder.Default
    private Instant joinedAt = Instant.now();
}
