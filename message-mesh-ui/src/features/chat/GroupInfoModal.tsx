import { useMemo, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Checkbox,
  Divider,
  HStack,
  IconButton,
  Input,
  Menu,
  MenuButton,
  MenuItem,
  MenuList,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalHeader,
  ModalOverlay,
  Spinner,
  Stack,
  Text,
  VStack,
} from '@chakra-ui/react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  FiCheck,
  FiEdit2,
  FiLogOut,
  FiMoreVertical,
  FiUserMinus,
  FiUserPlus,
  FiX,
} from 'react-icons/fi';
import { Avatar } from '../../components/Avatar';
import { PresenceBadge } from '../../components/PresenceBadge';
import { useConversationMembers } from '../../hooks/useConversationMembers';
import { useConversationMutations } from '../../hooks/useConversationMutations';
import { userApi } from '../../api/user.api';
import { usePresenceStore } from '../../store/presenceStore';
import { useAuthStore } from '../../store/authStore';
import type { ConversationDto } from '../../types/dto';

interface Props {
  conversation: ConversationDto;
  isOpen: boolean;
  onClose: () => void;
}

export function GroupInfoModal({ conversation, isOpen, onClose }: Props) {
  const navigate = useNavigate();
  const currentUsername = useAuthStore((s) => s.user?.username);
  const onlineUsers = usePresenceStore((s) => s.onlineUsers);
  const { data: members, isLoading } = useConversationMembers(conversation.id, isOpen);
  const { rename, addMembers, removeMember, updateMemberRole, remove } = useConversationMutations(
    conversation.id,
  );

  const [editingName, setEditingName] = useState(false);
  const [nameDraft, setNameDraft] = useState('');
  const [adding, setAdding] = useState(false);
  const [toAdd, setToAdd] = useState<string[]>([]);

  const title = conversation.title?.trim() || 'Group';
  const admins = members?.filter((m) => m.role === 'ADMIN') ?? [];
  const isAdmin = Boolean(
    members?.some((m) => m.username === currentUsername && m.role === 'ADMIN'),
  );
  const adminCount = admins.length;

  const { data: allUsers, isLoading: usersLoading } = useQuery({
    queryKey: ['users'],
    queryFn: userApi.list,
    enabled: isOpen && adding,
  });

  const candidates = useMemo(() => {
    const existing = new Set(members?.map((m) => m.username) ?? []);
    return (allUsers ?? []).filter((u) => !existing.has(u.username));
  }, [allUsers, members]);

  function startRename() {
    setNameDraft(conversation.title ?? '');
    setEditingName(true);
  }

  function saveRename() {
    const next = nameDraft.trim();
    if (!next || next === conversation.title) {
      setEditingName(false);
      return;
    }
    rename.mutate(next, { onSuccess: () => setEditingName(false) });
  }

  function toggleAdd(username: string) {
    setToAdd((prev) =>
      prev.includes(username) ? prev.filter((u) => u !== username) : [...prev, username],
    );
  }

  function confirmAdd() {
    if (toAdd.length === 0) return;
    addMembers.mutate(toAdd, {
      onSuccess: () => {
        setToAdd([]);
        setAdding(false);
      },
    });
  }

  function handleLeave() {
    remove.mutate(undefined, {
      onSuccess: () => {
        onClose();
        navigate('/chats');
      },
    });
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} isCentered size="md">
      <ModalOverlay />
      <ModalContent borderRadius="xl">
        <ModalHeader pb={2}>
          <HStack spacing={3} align="center">
            <Avatar name={title} size="md" isGroup />
            <Stack spacing={1} minW={0} flex={1}>
              {editingName ? (
                <HStack>
                  <Input
                    size="sm"
                    value={nameDraft}
                    onChange={(e) => setNameDraft(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') saveRename();
                      if (e.key === 'Escape') setEditingName(false);
                    }}
                    autoFocus
                  />
                  <IconButton
                    aria-label="Save name"
                    icon={<FiCheck />}
                    size="sm"
                    colorScheme="brand"
                    onClick={saveRename}
                    isLoading={rename.isPending}
                  />
                  <IconButton
                    aria-label="Cancel rename"
                    icon={<FiX />}
                    size="sm"
                    variant="ghost"
                    onClick={() => setEditingName(false)}
                  />
                </HStack>
              ) : (
                <HStack>
                  <Text noOfLines={1}>{title}</Text>
                  {isAdmin && (
                    <IconButton
                      aria-label="Rename group"
                      icon={<FiEdit2 />}
                      size="xs"
                      variant="ghost"
                      onClick={startRename}
                    />
                  )}
                </HStack>
              )}
              <Text fontSize="sm" fontWeight={400} color="text-muted">
                {conversation.memberCount} {conversation.memberCount === 1 ? 'member' : 'members'}
              </Text>
            </Stack>
          </HStack>
        </ModalHeader>
        <ModalCloseButton />
        <ModalBody pb={5}>
          {isAdmin && (
            <Box mb={3}>
              {!adding ? (
                <Button
                  leftIcon={<FiUserPlus />}
                  size="sm"
                  variant="outline"
                  w="full"
                  onClick={() => setAdding(true)}
                >
                  Add members
                </Button>
              ) : (
                <Stack spacing={2}>
                  <HStack justify="space-between">
                    <Text fontSize="sm" fontWeight={600}>
                      Add members
                    </Text>
                    <Button
                      size="xs"
                      variant="ghost"
                      onClick={() => {
                        setAdding(false);
                        setToAdd([]);
                      }}
                    >
                      Cancel
                    </Button>
                  </HStack>
                  {usersLoading ? (
                    <HStack justify="center" py={3}>
                      <Spinner size="sm" color="brand.500" />
                    </HStack>
                  ) : candidates.length > 0 ? (
                    <VStack align="stretch" spacing={1} maxH="180px" overflowY="auto">
                      {candidates.map((user) => (
                        <HStack
                          key={user.id}
                          px={2}
                          py={1.5}
                          borderRadius="lg"
                          cursor="pointer"
                          _hover={{ bg: 'blackAlpha.50', _dark: { bg: 'whiteAlpha.100' } }}
                          onClick={() => toggleAdd(user.username)}
                        >
                          <Checkbox
                            isChecked={toAdd.includes(user.username)}
                            pointerEvents="none"
                            colorScheme="brand"
                          />
                          <Avatar name={user.displayName} size="xs" />
                          <Stack spacing={0} flex={1} minW={0}>
                            <Text fontSize="sm" fontWeight={600} noOfLines={1}>
                              {user.displayName}
                            </Text>
                            <Text fontSize="xs" color="text-muted" noOfLines={1}>
                              @{user.username}
                            </Text>
                          </Stack>
                        </HStack>
                      ))}
                    </VStack>
                  ) : (
                    <Text fontSize="sm" color="text-muted" textAlign="center" py={2}>
                      Everyone is already in this group.
                    </Text>
                  )}
                  <Button
                    size="sm"
                    onClick={confirmAdd}
                    isDisabled={toAdd.length === 0}
                    isLoading={addMembers.isPending}
                  >
                    Add {toAdd.length > 0 ? `(${toAdd.length})` : ''}
                  </Button>
                </Stack>
              )}
              <Divider mt={3} />
            </Box>
          )}

          {isLoading ? (
            <HStack justify="center" py={6}>
              <Spinner color="brand.500" />
            </HStack>
          ) : members && members.length > 0 ? (
            <VStack align="stretch" spacing={1} maxH="320px" overflowY="auto">
              {members.map((member) => {
                const isSelf = member.username === currentUsername;
                const isLastAdmin = member.role === 'ADMIN' && adminCount <= 1;
                const showMenu = isAdmin && !isSelf;
                return (
                  <HStack key={member.userId} px={2} py={2} borderRadius="lg" spacing={3}>
                    <HStack position="relative">
                      <Avatar name={member.displayName} size="sm" />
                      <HStack position="absolute" bottom={0} right={0}>
                        <PresenceBadge online={Boolean(onlineUsers[member.username])} size="9px" />
                      </HStack>
                    </HStack>
                    <Stack spacing={0} flex={1} minW={0}>
                      <Text fontWeight={600} fontSize="sm" noOfLines={1}>
                        {member.displayName}
                        {isSelf && ' (You)'}
                      </Text>
                      <Text fontSize="xs" color="text-muted" noOfLines={1}>
                        @{member.username}
                      </Text>
                    </Stack>
                    {member.role === 'ADMIN' && (
                      <Badge colorScheme="brand" variant="subtle" flexShrink={0}>
                        Admin
                      </Badge>
                    )}
                    {showMenu && (
                      <Menu isLazy placement="bottom-end">
                        <MenuButton
                          as={IconButton}
                          aria-label="Member options"
                          icon={<FiMoreVertical />}
                          size="xs"
                          variant="ghost"
                        />
                        <MenuList>
                          {member.role === 'MEMBER' ? (
                            <MenuItem
                              icon={<FiCheck />}
                              onClick={() =>
                                updateMemberRole.mutate({ userId: member.userId, role: 'ADMIN' })
                              }
                            >
                              Make admin
                            </MenuItem>
                          ) : (
                            <MenuItem
                              icon={<FiX />}
                              isDisabled={isLastAdmin}
                              onClick={() =>
                                updateMemberRole.mutate({ userId: member.userId, role: 'MEMBER' })
                              }
                            >
                              Revoke admin
                            </MenuItem>
                          )}
                          <MenuItem
                            icon={<FiUserMinus />}
                            color="red.400"
                            isDisabled={isLastAdmin}
                            onClick={() => removeMember.mutate(member.userId)}
                          >
                            Remove
                          </MenuItem>
                        </MenuList>
                      </Menu>
                    )}
                  </HStack>
                );
              })}
            </VStack>
          ) : (
            <Text fontSize="sm" color="text-muted" textAlign="center" py={4}>
              No members to show.
            </Text>
          )}

          <Divider my={3} />
          <Button
            leftIcon={<FiLogOut />}
            variant="ghost"
            colorScheme="red"
            size="sm"
            w="full"
            onClick={handleLeave}
            isLoading={remove.isPending}
          >
            Leave group
          </Button>
        </ModalBody>
      </ModalContent>
    </Modal>
  );
}
