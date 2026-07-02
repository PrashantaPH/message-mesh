import { useQuery } from '@tanstack/react-query';
import { conversationApi } from '../api/conversation.api';

/** Paginated, case-insensitive search of a conversation's messages. */
export function useMessageSearch(conversationId: string, query: string, page = 0) {
  const q = query.trim();
  return useQuery({
    queryKey: ['message-search', conversationId, q, page],
    queryFn: () => conversationApi.search(conversationId, q, page),
    enabled: q.length > 0,
    placeholderData: (prev) => prev,
  });
}
