package com.message.mesh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit trail entry recording a sensitive action (who did what to
 * which target). Written by {@code AuditService} and surfaced to administrators.
 */
@Entity
@Table(name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_actor", columnList = "actorUsername"),
                @Index(name = "idx_audit_created", columnList = "createdAt")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, length = 64)
    private String actorUsername;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 32)
    private String targetType;

    @Column(length = 64)
    private String targetId;

    @Column(length = 512)
    private String details;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
