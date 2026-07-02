import { useMemo, useState } from 'react';
import {
  Button,
  Checkbox,
  HStack,
  Input,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Spinner,
  Stack,
  Text,
  VStack,
} from '@chakra-ui/react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Avatar } from '../../components/Avatar';
import { PresenceBadge } from '../../components/PresenceBadge';
import { userApi } from '../../api/user.api';
import { conversationApi } from '../../api/conversation.api';
import { usePresenceStore } from '../../store/presenceStore';
import type { ConversationDto, CreateConversationRequest } from '../../types/dto';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

export function NewConversationModal({ isOpen, onClose }: Props) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const onlineUsers = usePresenceStore((s) => s.onlineUsers);
  const [selected, setSelected] = useState<string[]>([]);
  const [title, setTitle] = useState('');

  const { data: users, isLoading } = useQuery({
    queryKey: ['users'],
    queryFn: userApi.list,
    enabled: isOpen,
  });

  const isGroup = selected.length > 1;

  const createMutation = useMutation({
    mutationFn: (payload: CreateConversationRequest) => conversationApi.create(payload),
    onSuccess: (conversation: ConversationDto) => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      reset();
      onClose();
      navigate(`/chats/${conversation.id}`);
    },
  });

  const selectedDisplayName = useMemo(() => {
    if (selected.length !== 1) return '';
    return users?.find((u) => u.username === selected[0])?.displayName ?? selected[0];
  }, [selected, users]);

  function toggle(username: string) {
    setSelected((prev) =>
      prev.includes(username) ? prev.filter((u) => u !== username) : [...prev, username],
    );
  }

  function reset() {
    setSelected([]);
    setTitle('');
  }

  function handleCreate() {
    if (selected.length === 0) return;
    createMutation.mutate({
      type: isGroup ? 'GROUP' : 'DIRECT',
      title: isGroup ? title.trim() || 'New group' : selectedDisplayName,
      memberUsernames: selected,
    });
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered size="md">
      <ModalOverlay />
      <ModalContent borderRadius="xl">
        <ModalHeader>New conversation</ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <Stack spacing={4}>
            {isGroup && (
              <Input
                placeholder="Group name"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            )}

            {isLoading ? (
              <HStack justify="center" py={6}>
                <Spinner color="brand.500" />
              </HStack>
            ) : users && users.length > 0 ? (
              <VStack align="stretch" spacing={1} maxH="320px" overflowY="auto">
                {users.map((user) => (
                  <HStack
                    key={user.id}
                    px={2}
                    py={2}
                    borderRadius="lg"
                    cursor="pointer"
                    _hover={{ bg: 'blackAlpha.50', _dark: { bg: 'whiteAlpha.100' } }}
                    onClick={() => toggle(user.username)}
                  >
                    <Checkbox
                      isChecked={selected.includes(user.username)}
                      pointerEvents="none"
                      colorScheme="brand"
                    />
                    <HStack position="relative">
                      <Avatar name={user.displayName} size="sm" />
                      <HStack position="absolute" bottom={0} right={0}>
                        <PresenceBadge online={Boolean(onlineUsers[user.username])} size="9px" />
                      </HStack>
                    </HStack>
                    <Stack spacing={0}>
                      <Text fontWeight={600} fontSize="sm">
                        {user.displayName}
                      </Text>
                      <Text fontSize="xs" color="text-muted">
                        @{user.username}
                      </Text>
                    </Stack>
                  </HStack>
                ))}
              </VStack>
            ) : (
              <Text fontSize="sm" color="text-muted" textAlign="center" py={4}>
                No other users yet. Invite someone to register!
              </Text>
            )}
          </Stack>
        </ModalBody>
        <ModalFooter>
          <Button variant="ghost" mr={3} onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={handleCreate}
            isDisabled={selected.length === 0}
            isLoading={createMutation.isPending}
          >
            {isGroup ? 'Create group' : 'Start chat'}
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}
