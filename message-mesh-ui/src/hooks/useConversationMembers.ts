import { useQuery } from '@tanstack/react-query';
import { conversationApi } from '../api/conversation.api';

export function useConversationMembers(conversationId: string | null, enabled = true) {
  return useQuery({
    queryKey: ['conversation-members', conversationId],
    queryFn: () => conversationApi.members(conversationId as string),
    enabled: Boolean(conversationId) && enabled,
    refetchOnWindowFocus: false,
  });
}
