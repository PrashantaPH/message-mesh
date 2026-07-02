import { axiosClient } from './axiosClient';
import type { MessageDto } from '../types/dto';

export const messageApi = {
  history: (conversationId: string, afterSeq = 0, limit = 50) =>
    axiosClient
      .get<MessageDto[]>(`/api/conversations/${conversationId}/messages`, {
        params: { afterSeq, limit },
      })
      .then((r) => r.data),

  edit: (messageId: string, body: string) =>
    axiosClient.patch<MessageDto>(`/api/messages/${messageId}`, { body }).then((r) => r.data),

  remove: (messageId: string) =>
    axiosClient.delete<MessageDto>(`/api/messages/${messageId}`).then((r) => r.data),

  react: (messageId: string, emoji: string) =>
    axiosClient
      .post<MessageDto>(`/api/messages/${messageId}/reactions`, { emoji })
      .then((r) => r.data),

  unreact: (messageId: string, emoji: string) =>
    axiosClient
      .delete<MessageDto>(`/api/messages/${messageId}/reactions/${encodeURIComponent(emoji)}`)
      .then((r) => r.data),
};
