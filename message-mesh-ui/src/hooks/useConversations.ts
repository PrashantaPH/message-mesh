import { useQuery } from '@tanstack/react-query';
import { conversationApi } from '../api/conversation.api';

export function useConversations() {
  return useQuery({
    queryKey: ['conversations'],
    queryFn: conversationApi.list,
    refetchOnWindowFocus: false,
  });
}
