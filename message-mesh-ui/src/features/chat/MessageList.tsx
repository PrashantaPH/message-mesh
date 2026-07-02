import { useEffect, useRef } from 'react';
import { Box, Center, Divider, Flex, Spinner, Text } from '@chakra-ui/react';
import { FiMessageSquare } from 'react-icons/fi';
import { MessageBubble } from './MessageBubble';
import { EmptyState } from '../../components/EmptyState';
import { useChatStore } from '../../store/chatStore';
import { useAuthStore } from '../../store/authStore';
import { formatDayLabel } from '../../utils/formatters';
import type { LocalMessage } from '../../types/dto';

interface Props {
  conversationId: string;
  isLoading: boolean;
  canModerate?: boolean;
}

function dayKey(iso: string): string {
  return new Date(iso).toDateString();
}

export function MessageList({ conversationId, isLoading, canModerate = false }: Props) {
  const messages = useChatStore((s) => s.messagesByConv[conversationId]) as
    | LocalMessage[]
    | undefined;
  const username = useAuthStore((s) => s.user?.username);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages?.length]);

  if (isLoading && !messages) {
    return (
      <Center flex={1}>
        <Spinner color="brand.500" />
      </Center>
    );
  }

  if (!messages || messages.length === 0) {
    return (
      <EmptyState
        icon={FiMessageSquare}
        title="No messages yet"
        description="Say hello and start the conversation."
      />
    );
  }

  let lastDay = '';
  let lastSender = '';

  return (
    <Flex direction="column" flex={1} overflowY="auto" py={3}>
      <Box w="full" maxW="3xl" mx="auto" px={{ base: 2, md: 4 }}>
        {messages.map((message) => {
          const thisDay = dayKey(message.createdAt);
          const showDay = thisDay !== lastDay;
          lastDay = thisDay;

          const isOwn = message.senderUsername === username;
          const showSender = message.senderUsername !== lastSender || showDay;
          lastSender = message.senderUsername;

          return (
            <Box key={message.clientTempId ?? message.id} id={`msg-${message.id}`}>
              {showDay && (
                <Center my={3}>
                  <Divider flex={1} />
                  <Text mx={3} fontSize="xs" color="text-muted" whiteSpace="nowrap">
                    {formatDayLabel(message.createdAt)}
                  </Text>
                  <Divider flex={1} />
                </Center>
              )}
              <MessageBubble
                message={message}
                isOwn={isOwn}
                showSender={showSender}
                canModerate={canModerate}
              />
            </Box>
          );
        })}
        <Box ref={bottomRef} />
      </Box>
    </Flex>
  );
}
