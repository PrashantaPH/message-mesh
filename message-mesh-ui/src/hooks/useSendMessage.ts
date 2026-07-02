import { useCallback } from 'react';
import { nanoidLike } from '../utils/id';
import { useAuthStore } from '../store/authStore';
import { useChatStore } from '../store/chatStore';
import { useSocket } from '../ws/SocketContext';
import type { LocalMessage } from '../types/dto';

export function useSendMessage(conversationId: string | null) {
  const username = useAuthStore((s) => s.user?.username);
  const { sendMessage } = useSocket();

  return useCallback(
    (body: string, parentId?: string | null) => {
      const trimmed = body.trim();
      if (!trimmed || !conversationId || !username) return;

      const clientTempId = nanoidLike();
      const optimistic: LocalMessage = {
        id: `temp-${clientTempId}`,
        conversationId,
        senderUsername: username,
        seq: Number.MAX_SAFE_INTEGER, // keep at the bottom until server assigns seq
        body: trimmed,
        status: 'SENDING',
        createdAt: new Date().toISOString(),
        clientTempId,
        deleted: false,
        reactions: [],
        parentId: parentId ?? null,
      };

      useChatStore.getState().addOptimistic(optimistic);
      sendMessage({ conversationId, body: trimmed, clientTempId, parentId: parentId ?? null });
    },
    [conversationId, username, sendMessage],
  );
}
