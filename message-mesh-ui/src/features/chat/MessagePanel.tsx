import { useEffect, useState } from 'react';
import { Flex } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { ConversationHeader } from './ConversationHeader';
import { MessageList } from './MessageList';
import { MessageSearchPanel } from './MessageSearchPanel';
import { TypingIndicator } from './TypingIndicator';
import { MessageComposer } from './MessageComposer';
import { EmptyState } from '../../components/EmptyState';
import { FiMessageCircle } from 'react-icons/fi';
import { useMessages } from '../../hooks/useMessages';
import { useConversations } from '../../hooks/useConversations';
import { useConversationMembers } from '../../hooks/useConversationMembers';
import { useChatStore } from '../../store/chatStore';
import { useAuthStore } from '../../store/authStore';
import { conversationApi } from '../../api/conversation.api';

const OPTIMISTIC_SEQ = Number.MAX_SAFE_INTEGER;

export function MessagePanel() {
  const { conversationId } = useParams();
  const { data: conversations } = useConversations();
  const { isLoading } = useMessages(conversationId ?? null);
  const setActiveConversation = useChatStore((s) => s.setActiveConversation);
  const messages = useChatStore((s) => (conversationId ? s.messagesByConv[conversationId] : undefined));
  const queryClient = useQueryClient();
  const [searchOpen, setSearchOpen] = useState(false);

  const username = useAuthStore((s) => s.user?.username);
  const conversation = conversations?.find((c) => c.id === conversationId);
  const isGroup = conversation?.type === 'GROUP';
  const { data: members } = useConversationMembers(conversationId ?? null, isGroup);
  const canModerate = Boolean(
    isGroup && members?.some((m) => m.username === username && m.role === 'ADMIN'),
  );

  // Close the search panel when switching conversations.
  useEffect(() => {
    setSearchOpen(false);
  }, [conversationId]);

  // Mark the conversation active. Live topic subscriptions are handled globally
  // by SocketProvider so messages arrive even when this panel is closed.
  useEffect(() => {
    if (!conversationId) return;
    setActiveConversation(conversationId);
    return () => {
      setActiveConversation(null);
    };
  }, [conversationId, setActiveConversation]);

  // Mark the conversation read as new messages arrive.
  const lastSeq = messages
    ? messages.reduce((max, m) => (m.seq !== OPTIMISTIC_SEQ ? Math.max(max, m.seq) : max), 0)
    : 0;

  useEffect(() => {
    if (!conversationId || lastSeq <= 0) return;
    conversationApi
      .markRead(conversationId, lastSeq)
      .then(() => queryClient.invalidateQueries({ queryKey: ['conversations'] }))
      .catch(() => undefined);
  }, [conversationId, lastSeq, queryClient]);

  if (!conversationId) {
    return (
      <EmptyState
        icon={FiMessageCircle}
        title="Select a conversation"
        description="Choose a chat from the list or start a new one."
      />
    );
  }

  return (
    <Flex direction="column" h="100%" bg="app-bg">
      {conversation && (
        <ConversationHeader
          conversation={conversation}
          onToggleSearch={() => setSearchOpen((v) => !v)}
        />
      )}
      {searchOpen && (
        <MessageSearchPanel conversationId={conversationId} onClose={() => setSearchOpen(false)} />
      )}
      <MessageList conversationId={conversationId} isLoading={isLoading} canModerate={canModerate} />
      <TypingIndicator conversationId={conversationId} />
      <MessageComposer conversationId={conversationId} />
    </Flex>
  );
}
