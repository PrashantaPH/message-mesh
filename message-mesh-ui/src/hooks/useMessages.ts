import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { messageApi } from '../api/message.api';
import { useChatStore } from '../store/chatStore';

/**
 * Loads message history for a conversation into the chat store. Live messages
 * are merged separately by the socket layer.
 */
export function useMessages(conversationId: string | null) {
  const query = useQuery({
    queryKey: ['messages', conversationId],
    queryFn: () => messageApi.history(conversationId as string, 0, 100),
    enabled: Boolean(conversationId),
    refetchOnWindowFocus: false,
  });

  useEffect(() => {
    if (conversationId && query.data) {
      useChatStore.getState().setMessages(conversationId, query.data);
    }
  }, [conversationId, query.data]);

  return query;
}
