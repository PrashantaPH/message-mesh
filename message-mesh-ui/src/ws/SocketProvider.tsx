import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import type { IMessage } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';
import { StompClient, type ConnectionStatus } from './StompClient';
import { SocketContext, type SocketContextValue } from './SocketContext';
import { STOMP } from '../types/stomp';
import { useAuthStore } from '../store/authStore';
import { useChatStore } from '../store/chatStore';
import { usePresenceStore } from '../store/presenceStore';
import { useConversations } from '../hooks/useConversations';
import { userApi } from '../api/user.api';
import { showMessageNotification } from '../utils/notifications';
import type {
  AckDto,
  ConversationDto,
  ConversationEvent,
  MessageDto,
  PresenceDto,
  SendMessageRequest,
  TypingEvent,
  TypingNotification,
} from '../types/dto';

export function SocketProvider({ children }: { children: ReactNode }) {
  const token = useAuthStore((s) => s.token);
  const currentUsername = useAuthStore((s) => s.user?.username);
  const clientRef = useRef<StompClient | null>(null);
  const [status, setStatus] = useState<ConnectionStatus>('idle');
  const queryClient = useQueryClient();
  const { data: conversations } = useConversations();
  // Stable key: only changes when the SET of conversations changes, not when a
  // single conversation's last message / unread count is mutated in the cache.
  const conversationIds = (conversations ?? []).map((c) => c.id).join(',');

  useEffect(() => {
    if (!token) return;

    const client = new StompClient(token);
    clientRef.current = client;
    const offStatus = client.onStatus(setStatus);

    client.subscribe(STOMP.topicPresence, (frame: IMessage) => {
      const presence: PresenceDto = JSON.parse(frame.body);
      usePresenceStore.getState().setOnline(presence.username, presence.online);
    });

    client.subscribe(STOMP.userAck, (frame: IMessage) => {
      const ack: AckDto = JSON.parse(frame.body);
      const activeId = useChatStore.getState().activeConversationId;
      if (activeId) {
        useChatStore.getState().updateStatus(activeId, ack.messageId, ack.status);
      }
    });

    // A conversation was created that includes this user (e.g. someone started a
    // group with them). Merge it into the list cache so it appears immediately
    // and the topic-subscription effect below subscribes to its messages.
    client.subscribe(STOMP.userConversations, (frame: IMessage) => {
      const conversation: ConversationDto = JSON.parse(frame.body);
      queryClient.setQueryData<ConversationDto[]>(['conversations'], (prev) => {
        if (!prev) return [conversation];
        if (prev.some((c) => c.id === conversation.id)) return prev;
        return [conversation, ...prev];
      });
    });

    client.activate();

    // Seed presence from REST snapshot.
    userApi
      .online()
      .then((usernames) => usePresenceStore.getState().setOnlineBulk(usernames))
      .catch(() => undefined);

    return () => {
      offStatus();
      client.deactivate();
      clientRef.current = null;
    };
  }, [token, queryClient]);

  const sendMessage = useCallback((payload: SendMessageRequest) => {
    clientRef.current?.publish(STOMP.send, payload);
  }, []);

  const sendTyping = useCallback((payload: TypingEvent) => {
    clientRef.current?.publish(STOMP.typing, payload);
  }, []);

  const ackMessage = useCallback((messageId: string) => {
    clientRef.current?.publish(STOMP.ack, { messageId });
  }, []);

  // Subscribe to ALL of the user's conversation topics so messages and typing
  // events arrive in real time even when a conversation is not currently open,
  // and keep the conversation-list cache (last message + unread) live without a
  // page refresh.
  useEffect(() => {
    const client = clientRef.current;
    if (!client || !conversationIds) return;

    const unsubscribers = conversationIds.split(',').flatMap((conversationId) => {
      const offMsg = client.subscribe(
        STOMP.topicConversation(conversationId),
        (frame: IMessage) => {
          const message: MessageDto = JSON.parse(frame.body);
          useChatStore.getState().upsertFromServer(message);

          const activeId = useChatStore.getState().activeConversationId;
          const isOwn = Boolean(currentUsername) && message.senderUsername === currentUsername;
          const isActive = activeId === message.conversationId;

          queryClient.setQueryData<ConversationDto[]>(['conversations'], (prev) => {
            if (!prev) return prev;
            const idx = prev.findIndex((c) => c.id === message.conversationId);
            if (idx === -1) return prev;
            const conv = prev[idx];
            const updated: ConversationDto = {
              ...conv,
              lastMessage: message,
              unreadCount: isOwn || isActive ? conv.unreadCount : conv.unreadCount + 1,
            };
            // Move the updated conversation to the top of the list.
            return [updated, ...prev.filter((_, i) => i !== idx)];
          });

          // Surface a desktop notification for messages from others while the
          // tab is unfocused (the helper no-ops otherwise).
          if (!isOwn) {
            const list = queryClient.getQueryData<ConversationDto[]>(['conversations']);
            const conv = list?.find((c) => c.id === message.conversationId);
            const isGroup = conv?.type === 'GROUP';
            showMessageNotification({
              title: isGroup && conv?.title ? conv.title : message.senderUsername,
              body: isGroup ? `${message.senderUsername}: ${message.body}` : message.body,
              tag: message.conversationId,
            });
          }

          // Auto-acknowledge messages received from other users.
          if (!isOwn) {
            client.publish(STOMP.ack, { messageId: message.id });
          }
        },
      );

      const offTyping = client.subscribe(
        STOMP.topicTyping(conversationId),
        (frame: IMessage) => {
          const typing: TypingNotification = JSON.parse(frame.body);
          if (currentUsername && typing.username !== currentUsername) {
            usePresenceStore.getState().setTyping(typing.conversationId, typing.username);
          }
        },
      );

      const offMeta = client.subscribe(
        STOMP.topicConversationMeta(conversationId),
        (frame: IMessage) => {
          const event: ConversationEvent = JSON.parse(frame.body);
          const removedSelf =
            event.type === 'MEMBER_REMOVED' &&
            Boolean(currentUsername) &&
            event.targetUsername === currentUsername;
          const conversationGone = event.type === 'CONVERSATION_DELETED' || removedSelf;

          if (conversationGone) {
            queryClient.setQueryData<ConversationDto[]>(['conversations'], (prev) =>
              prev ? prev.filter((c) => c.id !== event.conversationId) : prev,
            );
            if (useChatStore.getState().activeConversationId === event.conversationId) {
              useChatStore.getState().setActiveConversation(null);
            }
          } else {
            void queryClient.invalidateQueries({ queryKey: ['conversations'] });
          }
          void queryClient.invalidateQueries({
            queryKey: ['conversation-members', event.conversationId],
          });
        },
      );

      return [offMsg, offTyping, offMeta];
    });

    return () => unsubscribers.forEach((off) => off());
  }, [token, currentUsername, conversationIds, queryClient]);

  const value = useMemo<SocketContextValue>(
    () => ({ status, sendMessage, sendTyping, ackMessage }),
    [status, sendMessage, sendTyping, ackMessage],
  );

  return <SocketContext.Provider value={value}>{children}</SocketContext.Provider>;
}
