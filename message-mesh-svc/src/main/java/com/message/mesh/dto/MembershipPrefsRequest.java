package com.message.mesh.dto;

/**
 * Per-user conversation preferences. Fields are nullable so a caller can update
 * only the flag(s) they intend to change.
 */
public record MembershipPrefsRequest(
        Boolean muted,
        Boolean archived
) {
}
