import { useEffect, useState } from 'react';
import {
  Box,
  Center,
  Flex,
  HStack,
  IconButton,
  Input,
  InputGroup,
  InputLeftElement,
  Spinner,
  Stack,
  Text,
} from '@chakra-ui/react';
import { FiSearch, FiX } from 'react-icons/fi';
import { useMessageSearch } from '../../hooks/useMessageSearch';
import { formatDayLabel, formatTime } from '../../utils/formatters';

interface Props {
  conversationId: string;
  onClose: () => void;
}

export function MessageSearchPanel({ conversationId, onClose }: Props) {
  const [term, setTerm] = useState('');
  const [debounced, setDebounced] = useState('');

  useEffect(() => {
    const handle = setTimeout(() => setDebounced(term.trim()), 300);
    return () => clearTimeout(handle);
  }, [term]);

  const { data, isFetching } = useMessageSearch(conversationId, debounced);
  const results = data?.content ?? [];

  function jumpTo(messageId: string) {
    const el = document.getElementById(`msg-${messageId}`);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.style.transition = 'background-color 0.4s';
      el.style.backgroundColor = 'var(--chakra-colors-brand-100)';
      setTimeout(() => (el.style.backgroundColor = ''), 1200);
    }
  }

  return (
    <Box
      borderBottom="1px solid"
      borderColor="panel-border"
      bg="panel-bg"
      px={{ base: 3, md: 4 }}
      py={3}
    >
      <Flex w="full" maxW="3xl" mx="auto" direction="column" gap={2}>
        <HStack>
          <InputGroup>
            <InputLeftElement pointerEvents="none">
              <FiSearch />
            </InputLeftElement>
            <Input
              autoFocus
              placeholder="Search in this conversation"
              value={term}
              onChange={(e) => setTerm(e.target.value)}
              borderRadius="xl"
              bg="app-bg"
            />
          </InputGroup>
          <IconButton aria-label="Close search" icon={<FiX />} variant="ghost" onClick={onClose} />
        </HStack>

        {debounced && (
          <Box maxH="240px" overflowY="auto">
            {isFetching && !data ? (
              <Center py={4}>
                <Spinner size="sm" color="brand.500" />
              </Center>
            ) : results.length === 0 ? (
              <Text fontSize="sm" color="text-muted" py={2} textAlign="center">
                No messages found.
              </Text>
            ) : (
              <Stack spacing={1}>
                {results.map((m) => (
                  <Box
                    key={m.id}
                    px={3}
                    py={2}
                    borderRadius="lg"
                    cursor="pointer"
                    _hover={{ bg: 'blackAlpha.50', _dark: { bg: 'whiteAlpha.100' } }}
                    onClick={() => jumpTo(m.id)}
                  >
                    <HStack justify="space-between" spacing={2}>
                      <Text fontSize="xs" fontWeight={700} color="brand.400" noOfLines={1}>
                        {m.senderUsername}
                      </Text>
                      <Text fontSize="2xs" color="text-muted" flexShrink={0}>
                        {formatDayLabel(m.createdAt)} · {formatTime(m.createdAt)}
                      </Text>
                    </HStack>
                    <Text fontSize="sm" noOfLines={2}>
                      {m.body}
                    </Text>
                  </Box>
                ))}
              </Stack>
            )}
          </Box>
        )}
      </Flex>
    </Box>
  );
}
