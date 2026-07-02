package com.message.mesh.dto;

/**
 * Aggregate platform metrics for the admin dashboard.
 *
 * @param totalUsers           all registered accounts
 * @param activeUsers          accounts that are currently active (can sign in)
 * @param inactiveUsers        deactivated accounts
 * @param adminUsers           accounts with the ADMIN role
 * @param onlineUsers          accounts currently connected (live presence)
 * @param newUsersLast7Days    accounts created in the last 7 days
 * @param totalConversations   all conversations (including soft-deleted)
 * @param groupConversations   conversations of type GROUP
 * @param directConversations  conversations of type DIRECT
 * @param deletedConversations soft-deleted conversations
 * @param totalMessages        all persisted messages
 * @param messagesLast7Days    messages created in the last 7 days
 * @param auditEvents          total recorded audit-trail entries
 */
public record AdminStatsDto(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long adminUsers,
        long onlineUsers,
        long newUsersLast7Days,
        long totalConversations,
        long groupConversations,
        long directConversations,
        long deletedConversations,
        long totalMessages,
        long messagesLast7Days,
        long auditEvents
) {
}
