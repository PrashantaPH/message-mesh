package com.message.mesh.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Add one or more users (by username) to a group conversation. */
public record AddMembersRequest(
        @NotEmpty List<String> usernames
) {
}
