import { useState, type KeyboardEvent } from 'react';
import {
  Box,
  Button,
  HStack,
  Icon,
  IconButton,
  Menu,
  MenuButton,
  MenuItem,
  MenuList,
  Popover,
  PopoverBody,
  PopoverContent,
  PopoverTrigger,
  Text,
  Textarea,
  Tooltip,
  Wrap,
  WrapItem,
} from '@chakra-ui/react';
import {
  FiCheck,
  FiClock,
  FiAlertCircle,
  FiCornerUpLeft,
  FiEdit2,
  FiMoreVertical,
  FiSmile,
  FiTrash2,
} from 'react-icons/fi';
import { IoCheckmarkDone } from 'react-icons/io5';
import { formatTime } from '../../utils/formatters';
import { useAuthStore } from '../../store/authStore';
import { useChatStore } from '../../store/chatStore';
import { useMessageActions } from '../../hooks/useMessageActions';
import { REACTION_EMOJIS } from './reactions';
import type { LocalMessage } from '../../types/dto';

interface Props {
  message: LocalMessage;
  isOwn: boolean;
  showSender: boolean;
  canModerate?: boolean;
}

function StatusTicks({ status }: { status: LocalMessage['status'] }) {
  switch (status) {
    case 'SENDING':
      return <Icon as={FiClock} boxSize={3} aria-label="Sending" />;
    case 'FAILED':
      return <Icon as={FiAlertCircle} boxSize={3} color="red.300" aria-label="Failed" />;
    case 'SENT':
      return <Icon as={FiCheck} boxSize={3.5} aria-label="Sent" />;
    case 'DELIVERED':
      return <Icon as={IoCheckmarkDone} boxSize={3.5} aria-label="Delivered" />;
    case 'READ':
      return <Icon as={IoCheckmarkDone} boxSize={3.5} color="blue.200" aria-label="Read" />;
    default:
      return null;
  }
}

export function MessageBubble({ message, isOwn, showSender, canModerate = false }: Props) {
  const username = useAuthStore((s) => s.user?.username);
  const setReplyingTo = useChatStore((s) => s.setReplyingTo);
  const { edit, remove, react, unreact } = useMessageActions();
  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState(message.body);

  const isPersisted = !message.id.startsWith('temp-') && message.status !== 'SENDING';
  const canEdit = isOwn && isPersisted && !message.deleted;
  const canDelete = (isOwn || canModerate) && isPersisted && !message.deleted;

  function submitEdit() {
    const trimmed = editValue.trim();
    if (!trimmed || trimmed === message.body) {
      setIsEditing(false);
      return;
    }
    edit.mutate({ messageId: message.id, body: trimmed });
    setIsEditing(false);
  }

  function handleEditKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      submitEdit();
    } else if (e.key === 'Escape') {
      setIsEditing(false);
      setEditValue(message.body);
    }
  }

  function toggleReaction(emoji: string) {
    const mine = message.reactions?.find(
      (r) => r.emoji === emoji && username && r.usernames.includes(username),
    );
    if (mine) unreact.mutate({ messageId: message.id, emoji });
    else react.mutate({ messageId: message.id, emoji });
  }

  const actions = !message.deleted && (
    <MessageActions
      message={message}
      canEdit={canEdit}
      canDelete={canDelete}
      onReply={() => setReplyingTo(message.conversationId, message)}
      onEdit={() => {
        setEditValue(message.body);
        setIsEditing(true);
      }}
      onDelete={() => remove.mutate(message.id)}
      onReact={toggleReaction}
    />
  );

  return (
    <Box w="100%" py={0.5} role="group">
      {showSender && (
        <HStack spacing={2} mt={2} mb={1} px={1} justify={isOwn ? 'flex-end' : 'flex-start'}>
          {!isOwn && (
            <Text fontSize="xs" fontWeight={700} color="brand.400">
              {message.senderUsername}
            </Text>
          )}
          <Text fontSize="xs" color="text-muted">
            {formatTime(message.createdAt)}
          </Text>
        </HStack>
      )}
      <HStack w="100%" justify={isOwn ? 'flex-end' : 'flex-start'} spacing={1} align="flex-end">
        {isOwn && actions}
        <Box
          maxW={{ base: '80%', md: '65%' }}
          bg={isOwn ? 'bubble-own' : 'bubble-other'}
          color={isOwn ? 'white' : 'inherit'}
          px={3}
          py={2}
          borderRadius="2xl"
          borderBottomRightRadius={isOwn ? 'sm' : '2xl'}
          borderBottomLeftRadius={isOwn ? '2xl' : 'sm'}
          shadow="sm"
        >
          {message.parentPreview && (
            <Box
              mb={1.5}
              px={2}
              py={1}
              borderLeft="3px solid"
              borderColor={isOwn ? 'whiteAlpha.700' : 'brand.400'}
              bg={isOwn ? 'whiteAlpha.200' : 'blackAlpha.50'}
              _dark={{ bg: isOwn ? 'whiteAlpha.200' : 'whiteAlpha.100' }}
              borderRadius="md"
            >
              <Text fontSize="xs" fontWeight={700} noOfLines={1}>
                {message.parentPreview.senderUsername}
              </Text>
              <Text fontSize="xs" opacity={0.85} noOfLines={1}>
                {message.parentPreview.body || 'Message deleted'}
              </Text>
            </Box>
          )}

          {message.deleted ? (
            <Text as="i" opacity={0.7} fontSize="sm">
              This message was deleted
            </Text>
          ) : isEditing ? (
            <Box>
              <Textarea
                value={editValue}
                onChange={(e) => setEditValue(e.target.value)}
                onKeyDown={handleEditKeyDown}
                autoFocus
                rows={2}
                bg="app-bg"
                color="inherit"
                size="sm"
                borderRadius="md"
              />
              <HStack mt={1} justify="flex-end" spacing={2}>
                <Button size="xs" variant="ghost" onClick={() => setIsEditing(false)}>
                  Cancel
                </Button>
                <Button size="xs" onClick={submitEdit} isLoading={edit.isPending}>
                  Save
                </Button>
              </HStack>
            </Box>
          ) : (
            <Text whiteSpace="pre-wrap" wordBreak="break-word">
              {message.body}
              {message.editedAt && (
                <Text as="span" fontSize="2xs" opacity={0.7} ml={1}>
                  (edited)
                </Text>
              )}
            </Text>
          )}

          {!message.deleted && message.reactions && message.reactions.length > 0 && (
            <Wrap mt={1.5} spacing={1}>
              {message.reactions.map((r) => {
                const mine = Boolean(username && r.usernames.includes(username));
                return (
                  <WrapItem key={r.emoji}>
                    <Tooltip label={r.usernames.join(', ')} openDelay={300}>
                      <Button
                        size="xs"
                        variant={mine ? 'solid' : 'outline'}
                        colorScheme={mine ? 'brand' : 'gray'}
                        borderRadius="full"
                        px={2}
                        h={5}
                        minW="auto"
                        onClick={() => toggleReaction(r.emoji)}
                      >
                        <Text as="span" fontSize="xs">
                          {r.emoji} {r.count}
                        </Text>
                      </Button>
                    </Tooltip>
                  </WrapItem>
                );
              })}
            </Wrap>
          )}
        </Box>

        {!isOwn && actions}

        {isOwn && (
          <Box color="text-muted" pb={1}>
            <StatusTicks status={message.status} />
          </Box>
        )}
      </HStack>
    </Box>
  );
}

interface ActionsProps {
  message: LocalMessage;
  canEdit: boolean;
  canDelete: boolean;
  onReply: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onReact: (emoji: string) => void;
}

function MessageActions({ message, canEdit, canDelete, onReply, onEdit, onDelete, onReact }: ActionsProps) {
  const isPersisted = !message.id.startsWith('temp-') && message.status !== 'SENDING';
  if (!isPersisted) return null;

  return (
    <HStack
      spacing={0.5}
      opacity={0}
      _groupHover={{ opacity: 1 }}
      transition="opacity 0.15s"
      alignSelf="center"
    >
      <Popover placement="top" isLazy>
        <PopoverTrigger>
          <IconButton
            aria-label="Add reaction"
            icon={<FiSmile />}
            size="xs"
            variant="ghost"
            borderRadius="full"
          />
        </PopoverTrigger>
        <PopoverContent w="auto">
          <PopoverBody px={2} py={1}>
            <HStack spacing={1}>
              {REACTION_EMOJIS.map((emoji) => (
                <Button
                  key={emoji}
                  size="sm"
                  variant="ghost"
                  px={1}
                  minW="auto"
                  onClick={() => onReact(emoji)}
                >
                  <Text fontSize="lg">{emoji}</Text>
                </Button>
              ))}
            </HStack>
          </PopoverBody>
        </PopoverContent>
      </Popover>
      <Menu isLazy>
        <MenuButton
          as={IconButton}
          aria-label="Message actions"
          icon={<FiMoreVertical />}
          size="xs"
          variant="ghost"
          borderRadius="full"
        />
        <MenuList>
          <MenuItem icon={<FiCornerUpLeft />} onClick={onReply}>
            Reply
          </MenuItem>
          {canEdit && (
            <MenuItem icon={<FiEdit2 />} onClick={onEdit}>
              Edit
            </MenuItem>
          )}
          {canDelete && (
            <MenuItem icon={<FiTrash2 />} color="red.400" onClick={onDelete}>
              Delete
            </MenuItem>
          )}
        </MenuList>
      </Menu>
    </HStack>
  );
}
