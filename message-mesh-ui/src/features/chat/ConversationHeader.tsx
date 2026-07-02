import { useRef } from 'react';
import {
  AlertDialog,
  AlertDialogBody,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogOverlay,
  Badge,
  Box,
  Button,
  Flex,
  HStack,
  Icon,
  IconButton,
  Menu,
  MenuButton,
  MenuDivider,
  MenuItem,
  MenuList,
  Stack,
  Text,
  useDisclosure,
} from '@chakra-ui/react';
import {
  FiArrowLeft,
  FiArchive,
  FiBellOff,
  FiBell,
  FiLogOut,
  FiMoreVertical,
  FiSearch,
  FiTrash2,
} from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { Avatar } from '../../components/Avatar';
import { useTypingUsers } from '../../hooks/usePresence';
import { useConversationMutations } from '../../hooks/useConversationMutations';
import type { ConversationDto } from '../../types/dto';
import { GroupInfoModal } from './GroupInfoModal';

interface Props {
  conversation: ConversationDto;
  onToggleSearch?: () => void;
}

export function ConversationHeader({ conversation, onToggleSearch }: Props) {
  const navigate = useNavigate();
  const title = conversation.title?.trim() || 'Direct message';
  const typingUsers = useTypingUsers(conversation.id);
  const isGroup = conversation.type === 'GROUP';
  const info = useDisclosure();
  const confirmDelete = useDisclosure();
  const cancelRef = useRef<HTMLButtonElement>(null);
  const { setPrefs, remove } = useConversationMutations(conversation.id);

  const memberLabel = `${conversation.memberCount} ${
    conversation.memberCount === 1 ? 'member' : 'members'
  }`;

  const subtitle =
    typingUsers.length > 0
      ? `${typingUsers.join(', ')} typing…`
      : isGroup
        ? memberLabel
        : 'Direct message';

  const deleteLabel = isGroup ? 'Delete / leave group' : 'Delete conversation';

  function handleDelete() {
    remove.mutate(undefined, {
      onSuccess: () => {
        confirmDelete.onClose();
        navigate('/chats');
      },
    });
  }

  return (
    <Box borderBottom="1px solid" borderColor="panel-border" bg="panel-bg">
      <Flex w="full" maxW="3xl" mx="auto" px={{ base: 3, md: 4 }} py={3} align="center" gap={2}>
        <IconButton
          aria-label="Back"
          icon={<Icon as={FiArrowLeft} />}
          variant="ghost"
          display={{ base: 'inline-flex', md: 'none' }}
          onClick={() => navigate('/chats')}
        />
        <HStack
          spacing={2}
          flex={1}
          minW={0}
          align="center"
          cursor={isGroup ? 'pointer' : 'default'}
          onClick={isGroup ? info.onOpen : undefined}
          role={isGroup ? 'button' : undefined}
          aria-label={isGroup ? 'View group info' : undefined}
        >
          <Avatar name={title} size="sm" isGroup={isGroup} />
          <Stack spacing={0} flex={1} minW={0}>
            <HStack spacing={2}>
              <Text fontWeight={700} noOfLines={1}>
                {title}
              </Text>
              {isGroup && (
                <Badge colorScheme="brand" variant="subtle">
                  Group
                </Badge>
              )}
              {conversation.muted && (
                <Icon as={FiBellOff} color="text-muted" boxSize={3.5} aria-label="Muted" />
              )}
            </HStack>
            <Text
              fontSize="xs"
              color={typingUsers.length > 0 ? 'brand.400' : 'text-muted'}
              noOfLines={1}
            >
              {subtitle}
            </Text>
          </Stack>
        </HStack>

        <IconButton
          aria-label="Search messages"
          icon={<FiSearch />}
          variant="ghost"
          onClick={onToggleSearch}
        />
        <Menu isLazy>
          <MenuButton
            as={IconButton}
            aria-label="Conversation options"
            icon={<FiMoreVertical />}
            variant="ghost"
          />
          <MenuList>
            <MenuItem
              icon={conversation.muted ? <FiBell /> : <FiBellOff />}
              onClick={() => setPrefs.mutate({ muted: !conversation.muted })}
            >
              {conversation.muted ? 'Unmute' : 'Mute'}
            </MenuItem>
            <MenuItem
              icon={<FiArchive />}
              onClick={() =>
                setPrefs.mutate(
                  { archived: true },
                  { onSuccess: () => navigate('/chats') },
                )
              }
            >
              Archive
            </MenuItem>
            <MenuDivider />
            <MenuItem
              icon={isGroup ? <FiLogOut /> : <FiTrash2 />}
              color="red.400"
              onClick={confirmDelete.onOpen}
            >
              {deleteLabel}
            </MenuItem>
          </MenuList>
        </Menu>
      </Flex>

      {isGroup && (
        <GroupInfoModal conversation={conversation} isOpen={info.isOpen} onClose={info.onClose} />
      )}

      <AlertDialog
        isOpen={confirmDelete.isOpen}
        leastDestructiveRef={cancelRef}
        onClose={confirmDelete.onClose}
        isCentered
      >
        <AlertDialogOverlay>
          <AlertDialogContent borderRadius="xl">
            <AlertDialogHeader fontSize="lg" fontWeight="bold">
              {deleteLabel}
            </AlertDialogHeader>
            <AlertDialogBody>
              {isGroup
                ? 'Group admins delete this group for everyone; other members simply leave. This cannot be undone.'
                : 'This removes the conversation from your list. This cannot be undone.'}
            </AlertDialogBody>
            <AlertDialogFooter>
              <Button ref={cancelRef} onClick={confirmDelete.onClose} variant="ghost">
                Cancel
              </Button>
              <Button colorScheme="red" ml={3} onClick={handleDelete} isLoading={remove.isPending}>
                {isGroup ? 'Leave / delete' : 'Delete'}
              </Button>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialogOverlay>
      </AlertDialog>
    </Box>
  );
}
