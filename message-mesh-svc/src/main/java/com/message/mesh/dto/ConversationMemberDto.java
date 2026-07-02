package com.message.mesh.dto;

import com.message.mesh.domain.User;
import com.message.mesh.enums.MembershipRole;

import java.util.UUID;

/**
 * A single participant of a conversation, enriched with the participant's
 * membership role so clients can distinguish admins from regular members.
 */
public record ConversationMemberDto(
        UUID userId,
        String username,
        String displayName,
        MembershipRole role
) {

    public static ConversationMemberDto of(User user, MembershipRole role) {
        return new ConversationMemberDto(user.getId(), user.getUsername(), user.getDisplayName(), role);
    }
}
