import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { adminApi, type ListConversationsParams } from '../api/admin.api';

const ADMIN_CONVERSATIONS_KEY = ['admin', 'conversations'] as const;

export function useAdminConversations(params: ListConversationsParams = {}) {
  const { q = '', type, deleted, page = 0, size = 20 } = params;
  return useQuery({
    queryKey: [
      ...ADMIN_CONVERSATIONS_KEY,
      { q, type: type ?? null, deleted: deleted ?? null, page, size },
    ],
    queryFn: () => adminApi.listConversations({ q: q || undefined, type, deleted, page, size }),
    refetchOnWindowFocus: false,
    placeholderData: keepPreviousData,
  });
}

export function useAdminConversationMessages(id: string | null) {
  return useQuery({
    queryKey: [...ADMIN_CONVERSATIONS_KEY, 'messages', id],
    queryFn: () => adminApi.listConversationMessages(id as string),
    enabled: Boolean(id),
    refetchOnWindowFocus: false,
  });
}

export function useAdminConversationMutations() {
  const queryClient = useQueryClient();
  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ADMIN_CONVERSATIONS_KEY });

  return {
    deleteConversation: useMutation({
      mutationFn: (id: string) => adminApi.deleteConversation(id),
      onSuccess: invalidate,
    }),
    restoreConversation: useMutation({
      mutationFn: (id: string) => adminApi.restoreConversation(id),
      onSuccess: invalidate,
    }),
    deleteMessage: useMutation({
      mutationFn: ({ conversationId, messageId }: { conversationId: string; messageId: string }) =>
        adminApi.deleteConversationMessage(conversationId, messageId),
      onSuccess: (_data, variables) => {
        invalidate();
        queryClient.invalidateQueries({
          queryKey: [...ADMIN_CONVERSATIONS_KEY, 'messages', variables.conversationId],
        });
      },
    }),
  };
}
