import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@chakra-ui/react';
import { conversationApi } from '../api/conversation.api';
import type { MembershipPrefsRequest, MembershipRole } from '../types/dto';

/**
 * Group-management and per-user preference mutations. Meta changes are also
 * broadcast over STOMP, but we invalidate the relevant caches here so the acting
 * user's UI updates immediately.
 */
export function useConversationMutations(conversationId: string) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['conversations'] });
    queryClient.invalidateQueries({ queryKey: ['conversation-members', conversationId] });
  };

  const rename = useMutation({
    mutationFn: (title: string) => conversationApi.rename(conversationId, title),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Group renamed', status: 'success', duration: 2000 });
    },
  });

  const addMembers = useMutation({
    mutationFn: (usernames: string[]) => conversationApi.addMembers(conversationId, usernames),
    onSuccess: () => {
      invalidate();
      toast({ title: 'Members added', status: 'success', duration: 2000 });
    },
    onError: () => toast({ title: 'Could not add members', status: 'error', duration: 3000 }),
  });

  const removeMember = useMutation({
    mutationFn: (userId: string) => conversationApi.removeMember(conversationId, userId),
    onSuccess: invalidate,
    onError: () => toast({ title: 'Could not remove member', status: 'error', duration: 3000 }),
  });

  const updateMemberRole = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: MembershipRole }) =>
      conversationApi.updateMemberRole(conversationId, userId, role),
    onSuccess: invalidate,
    onError: () => toast({ title: 'Could not change role', status: 'error', duration: 3000 }),
  });

  const setPrefs = useMutation({
    mutationFn: (prefs: MembershipPrefsRequest) => conversationApi.setPrefs(conversationId, prefs),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['conversations'] }),
  });

  const remove = useMutation({
    mutationFn: () => conversationApi.remove(conversationId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['conversations'] }),
    onError: () => toast({ title: 'Could not delete conversation', status: 'error', duration: 3000 }),
  });

  return { rename, addMembers, removeMember, updateMemberRole, setPrefs, remove };
}
