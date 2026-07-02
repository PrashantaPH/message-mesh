import { axiosClient } from './axiosClient';
import type {
  AdminConversationDto,
  AdminStatsDto,
  AdminUserDetailDto,
  AdminUserDto,
  AuditEventDto,
  ConversationType,
  CreateUserRequest,
  MessageDto,
  PagedResponse,
  UserRole,
} from '../types/dto';

export interface ListUsersParams {
  q?: string;
  role?: UserRole;
  active?: boolean;
  page?: number;
  size?: number;
}

export interface ListConversationsParams {
  q?: string;
  type?: ConversationType;
  deleted?: boolean;
  page?: number;
  size?: number;
}

export interface ListAuditParams {
  actor?: string;
  action?: string;
  from?: string; // ISO instant
  to?: string; // ISO instant
  page?: number;
  size?: number;
}

export const adminApi = {
  listUsers: ({ q, role, active, page = 0, size = 20 }: ListUsersParams = {}) =>
    axiosClient
      .get<PagedResponse<AdminUserDto>>('/api/admin/users', {
        params: {
          ...(q ? { q } : {}),
          ...(role ? { role } : {}),
          ...(active !== undefined ? { active } : {}),
          page,
          size,
        },
      })
      .then((r) => r.data),

  getUserDetail: (id: string) =>
    axiosClient.get<AdminUserDetailDto>(`/api/admin/users/${id}`).then((r) => r.data),

  createUser: (payload: CreateUserRequest) =>
    axiosClient.post<AdminUserDto>('/api/admin/users', payload).then((r) => r.data),

  updateRole: (id: string, role: UserRole) =>
    axiosClient
      .patch<AdminUserDto>(`/api/admin/users/${id}/role`, { role })
      .then((r) => r.data),

  updateStatus: (id: string, active: boolean) =>
    axiosClient
      .patch<AdminUserDto>(`/api/admin/users/${id}/status`, { active })
      .then((r) => r.data),

  resetPassword: (id: string, newPassword: string) =>
    axiosClient
      .post<void>(`/api/admin/users/${id}/reset-password`, { newPassword })
      .then((r) => r.data),

  revokeSessions: (id: string) =>
    axiosClient
      .post<AdminUserDto>(`/api/admin/users/${id}/revoke-sessions`)
      .then((r) => r.data),

  deleteUser: (id: string) =>
    axiosClient.delete<void>(`/api/admin/users/${id}`).then((r) => r.data),

  getStats: () => axiosClient.get<AdminStatsDto>('/api/admin/stats').then((r) => r.data),

  listConversations: ({ q, type, deleted, page = 0, size = 20 }: ListConversationsParams = {}) =>
    axiosClient
      .get<PagedResponse<AdminConversationDto>>('/api/admin/conversations', {
        params: {
          ...(q ? { q } : {}),
          ...(type ? { type } : {}),
          ...(deleted !== undefined ? { deleted } : {}),
          page,
          size,
        },
      })
      .then((r) => r.data),

  deleteConversation: (id: string) =>
    axiosClient
      .delete<AdminConversationDto>(`/api/admin/conversations/${id}`)
      .then((r) => r.data),

  restoreConversation: (id: string) =>
    axiosClient
      .post<AdminConversationDto>(`/api/admin/conversations/${id}/restore`)
      .then((r) => r.data),

  listConversationMessages: (id: string, page = 0, size = 50) =>
    axiosClient
      .get<PagedResponse<MessageDto>>(`/api/admin/conversations/${id}/messages`, {
        params: { page, size },
      })
      .then((r) => r.data),

  deleteConversationMessage: (conversationId: string, messageId: string) =>
    axiosClient
      .delete<MessageDto>(`/api/admin/conversations/${conversationId}/messages/${messageId}`)
      .then((r) => r.data),

  listAudit: ({ actor, action, from, to, page = 0, size = 20 }: ListAuditParams = {}) =>
    axiosClient
      .get<PagedResponse<AuditEventDto>>('/api/admin/audit', {
        params: {
          ...(actor ? { actor } : {}),
          ...(action ? { action } : {}),
          ...(from ? { from } : {}),
          ...(to ? { to } : {}),
          page,
          size,
        },
      })
      .then((r) => r.data),
};
