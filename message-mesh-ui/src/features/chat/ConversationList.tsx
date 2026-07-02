import { useMemo, useState } from 'react';
import {
  Box,
  Button,
  Flex,
  Heading,
  HStack,
  Icon,
  Input,
  InputGroup,
  InputLeftElement,
  Spinner,
  Stack,
  useDisclosure,
} from '@chakra-ui/react';
import { FiPlus, FiSearch, FiInbox } from 'react-icons/fi';
import { useNavigate, useParams } from 'react-router-dom';
import { ConversationItem } from './ConversationItem';
import { NewConversationModal } from './NewConversationModal';
import { EmptyState } from '../../components/EmptyState';
import { useConversations } from '../../hooks/useConversations';

export function ConversationList() {
  const { data, isLoading } = useConversations();
  const navigate = useNavigate();
  const { conversationId } = useParams();
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [query, setQuery] = useState('');

  const filtered = useMemo(() => {
    if (!data) return [];
    const q = query.trim().toLowerCase();
    if (!q) return data;
    return data.filter((c) => (c.title ?? '').toLowerCase().includes(q));
  }, [data, query]);

  return (
    <Flex direction="column" h="100%">
      <Box px={4} pt={4} pb={3}>
        <HStack justify="space-between" mb={3}>
          <Heading size="md">Chats</Heading>
          <Button size="sm" leftIcon={<Icon as={FiPlus} />} onClick={onOpen}>
            New
          </Button>
        </HStack>
        <InputGroup size="sm">
          <InputLeftElement pointerEvents="none">
            <Icon as={FiSearch} color="text-muted" />
          </InputLeftElement>
          <Input
            placeholder="Search conversations"
            borderRadius="full"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </InputGroup>
      </Box>

      <Box
        flex={1}
        overflowY="auto"
        overflowX="hidden"
        px={2}
        pb={2}
        sx={{
          overscrollBehavior: 'contain',
          scrollbarGutter: 'stable',
          // Reveal scrollbar thumb only while hovering the list
          '&::-webkit-scrollbar-thumb': { background: 'transparent' },
          '&:hover::-webkit-scrollbar-thumb': {
            background: 'var(--chakra-colors-blackAlpha-300)',
            backgroundClip: 'content-box',
          },
          _dark: {
            '&:hover::-webkit-scrollbar-thumb': {
              background: 'var(--chakra-colors-whiteAlpha-300)',
              backgroundClip: 'content-box',
            },
          },
        }}
      >
        {isLoading ? (
          <Flex justify="center" py={10}>
            <Spinner color="brand.500" />
          </Flex>
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={FiInbox}
            title="No conversations"
            description="Start a new chat to begin messaging."
          />
        ) : (
          <Stack spacing={1}>
            {filtered.map((conversation) => (
              <ConversationItem
                key={conversation.id}
                conversation={conversation}
                isActive={conversation.id === conversationId}
                onClick={() => navigate(`/chats/${conversation.id}`)}
              />
            ))}
          </Stack>
        )}
      </Box>

      <NewConversationModal isOpen={isOpen} onClose={onClose} />
    </Flex>
  );
}
