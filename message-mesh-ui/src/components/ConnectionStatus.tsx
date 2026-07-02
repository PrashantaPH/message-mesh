import { Box, HStack, Spinner, Text } from '@chakra-ui/react';
import { useSocket } from '../ws/SocketContext';

const LABEL: Record<string, string> = {
  connecting: 'Connecting…',
  reconnecting: 'Reconnecting…',
  error: 'Connection lost — retrying…',
};

export function ConnectionStatus() {
  const { status } = useSocket();
  if (status === 'connected' || status === 'idle') return null;

  const isError = status === 'error';
  return (
    <Box
      bg={isError ? 'red.500' : 'orange.400'}
      color="white"
      px={4}
      py={1.5}
      fontSize="sm"
      textAlign="center"
    >
      <HStack justify="center" spacing={2}>
        <Spinner size="xs" />
        <Text>{LABEL[status] ?? 'Working…'}</Text>
      </HStack>
    </Box>
  );
}
