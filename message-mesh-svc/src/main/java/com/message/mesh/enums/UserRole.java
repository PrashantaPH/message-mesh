package com.message.mesh.enums;

/**
 * Global application-level role of a user. Distinct from the conversation-scoped
 * {@link MembershipRole}. ADMIN users may access the user-management (admin) APIs.
 */
public enum UserRole {
    USER,
    ADMIN
}
