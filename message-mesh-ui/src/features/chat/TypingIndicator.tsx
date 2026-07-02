import { Box, HStack, Text } from '@chakra-ui/react';
import { keyframes } from '@emotion/react';
import { useTypingUsers } from '../../hooks/usePresence';

const bounce = keyframes`
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
`;

function Dot({ delay }: { delay: string }) {
  return (
    <HStack
      as="span"
      w="6px"
      h="6px"
      borderRadius="full"
      bg="text-muted"
      animation={`${bounce} 1.3s infinite ease-in-out`}
      sx={{ animationDelay: delay }}
    />
  );
}

export function TypingIndicator({ conversationId }: { conversationId: string }) {
  const typingUsers = useTypingUsers(conversationId);
  if (typingUsers.length === 0) return null;

  const label =
    typingUsers.length === 1
      ? `${typingUsers[0]} is typing`
      : `${typingUsers.length} people are typing`;

  return (
    <Box w="full" maxW="3xl" mx="auto" px={{ base: 3, md: 4 }}>
      <HStack py={1.5} spacing={2} color="text-muted">
        <HStack spacing={1}>
          <Dot delay="0s" />
          <Dot delay="0.2s" />
          <Dot delay="0.4s" />
        </HStack>
        <Text fontSize="sm">{label}…</Text>
      </HStack>
    </Box>
  );
}
