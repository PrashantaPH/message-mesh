import { axiosClient } from './axiosClient';
import type {
  AddMembersRequest,
  ConversationDto,
  ConversationMemberDto,
  CreateConversationRequest,
  MembershipPrefsRequest,
  MembershipRole,
  MessageDto,
  PagedResponse,
} from '../types/dto';

export const conversationApi = {
  list: () => axiosClient.get<ConversationDto[]>('/api/conversations').then((r) => r.data),

  create: (payload: CreateConversationRequest) =>
    axiosClient.post<ConversationDto>('/api/conversations', payload).then((r) => r.data),

  markRead: (conversationId: string, seq: number) =>
    axiosClient.post<void>(`/api/conversations/${conversationId}/read`, { seq }).then((r) => r.data),

  members: (conversationId: string) =>
    axiosClient
      .get<ConversationMemberDto[]>(`/api/conversations/${conversationId}/members`)
      .then((r) => r.data),

  rename: (conversationId: string, title: string) =>
    axiosClient
      .patch<ConversationDto>(`/api/conversations/${conversationId}`, { title })
      .then((r) => r.data),

  addMembers: (conversationId: string, usernames: string[]) =>
    axiosClient
      .post<ConversationMemberDto[]>(`/api/conversations/${conversationId}/members`, {
        usernames,
      } satisfies AddMembersRequest)
      .then((r) => r.data),

  removeMember: (conversationId: string, userId: string) =>
    axiosClient
      .delete<void>(`/api/conversations/${conversationId}/members/${userId}`)
      .then((r) => r.data),

  updateMemberRole: (conversationId: string, userId: string, role: MembershipRole) =>
    axiosClient
      .patch<ConversationMemberDto[]>(
        `/api/conversations/${conversationId}/members/${userId}/role`,
        { role },
      )
      .then((r) => r.data),

  setPrefs: (conversationId: string, prefs: MembershipPrefsRequest) =>
    axiosClient
      .patch<ConversationDto>(`/api/conversations/${conversationId}/membership`, prefs)
      .then((r) => r.data),

  remove: (conversationId: string) =>
    axiosClient.delete<void>(`/api/conversations/${conversationId}`).then((r) => r.data),

  search: (conversationId: string, q: string, page = 0, size = 20) =>
    axiosClient
      .get<PagedResponse<MessageDto>>(`/api/conversations/${conversationId}/messages/search`, {
        params: { q, page, size },
      })
      .then((r) => r.data),
};
