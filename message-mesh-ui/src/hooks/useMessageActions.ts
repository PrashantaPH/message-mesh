import { useMutation } from '@tanstack/react-query';
import { useToast } from '@chakra-ui/react';
import { messageApi } from '../api/message.api';
import { useChatStore } from '../store/chatStore';
import type { MessageDto } from '../types/dto';

/**
 * Message-level mutations (edit / delete / react). The server also re-broadcasts
 * the updated message over STOMP, but we optimistically apply the REST response
 * so the acting user sees the change instantly even before the frame arrives.
 */
export function useMessageActions() {
  const toast = useToast();

  const apply = (message: MessageDto) => useChatStore.getState().upsertFromServer(message);

  const edit = useMutation({
    mutationFn: ({ messageId, body }: { messageId: string; body: string }) =>
      messageApi.edit(messageId, body),
    onSuccess: apply,
    onError: () => toast({ title: 'Could not edit message', status: 'error', duration: 3000 }),
  });

  const remove = useMutation({
    mutationFn: (messageId: string) => messageApi.remove(messageId),
    onSuccess: apply,
    onError: () => toast({ title: 'Could not delete message', status: 'error', duration: 3000 }),
  });

  const react = useMutation({
    mutationFn: ({ messageId, emoji }: { messageId: string; emoji: string }) =>
      messageApi.react(messageId, emoji),
    onSuccess: apply,
  });

  const unreact = useMutation({
    mutationFn: ({ messageId, emoji }: { messageId: string; emoji: string }) =>
      messageApi.unreact(messageId, emoji),
    onSuccess: apply,
  });

  return { edit, remove, react, unreact };
}
