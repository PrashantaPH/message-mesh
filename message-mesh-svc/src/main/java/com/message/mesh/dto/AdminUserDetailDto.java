package com.message.mesh.dto;

import java.util.List;

/**
 * Detailed admin projection of a single user, combining the standard admin user
 * fields with the conversations they belong to (for the admin drill-down view).
 */
public record AdminUserDetailDto(
        AdminUserDto user,
        List<AdminConversationDto> conversations
) {
}
