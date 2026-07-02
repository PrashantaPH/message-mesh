package com.message.mesh.dto;

import com.message.mesh.domain.AuditEvent;

import java.time.Instant;
import java.util.UUID;

/** Read projection of an audit trail entry for the admin console. */
public record AuditEventDto(
        UUID id,
        String actorUsername,
        String action,
        String targetType,
        String targetId,
        String details,
        Instant createdAt
) {

    public static AuditEventDto from(AuditEvent e) {
        return new AuditEventDto(
                e.getId(),
                e.getActorUsername(),
                e.getAction(),
                e.getTargetType(),
                e.getTargetId(),
                e.getDetails(),
                e.getCreatedAt());
    }
}
