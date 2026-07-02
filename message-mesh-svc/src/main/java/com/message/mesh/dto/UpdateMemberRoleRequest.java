package com.message.mesh.dto;

import com.message.mesh.enums.MembershipRole;
import jakarta.validation.constraints.NotNull;

/** Promote or demote a group member (ADMIN or MEMBER). */
public record UpdateMemberRoleRequest(
        @NotNull MembershipRole role
) {
}
