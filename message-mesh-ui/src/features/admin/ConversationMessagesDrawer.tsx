import { useRef, useState } from 'react';
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
  Center,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerHeader,
  DrawerOverlay,
  Flex,
  HStack,
  IconButton,
  Spinner,
  Text,
  useToast,
  VStack,
} from '@chakra-ui/react';
import { FiTrash2 } from 'react-icons/fi';
import {
  useAdminConversationMessages,
  useAdminConversationMutations,
} from '../../hooks/useAdminConversations';
import { formatDayLabel, formatTime } from '../../utils/formatters';
import type { AdminConversationDto, MessageDto } from '../../types/dto';

function errorMessage(error: unknown, fallback: string): string {
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
  return message ?? fallback;
}

interface Props {
  conversation: AdminConversationDto | null;
  onClose: () => void;
}

export function ConversationMessagesDrawer({ conversation, onClose }: Props) {
  const toast = useToast();
  const id = conversation?.id ?? null;
  const { data, isLoading, isError } = useAdminConversationMessages(id);
  const { deleteMessage } = useAdminConversationMutations();

  const [target, setTarget] = useState<MessageDto | null>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);

  const messages = data?.content ?? [];

  const confirmDelete = () => {
    if (!target || !id) return;
    deleteMessage.mutate(
      { conversationId: id, messageId: target.id },
      {
        onSuccess: () => {
          toast({ status: 'success', title: 'Message deleted', duration: 2000 });
          setTarget(null);
        },
        onError: (e) =>
          toast({ status: 'error', title: errorMessage(e, 'Could not delete message'), duration: 4000 }),
      },
    );
  };

  return (
    <>
      <Drawer isOpen={Boolean(conversation)} placement="right" onClose={onClose} size="md">
        <DrawerOverlay />
        <DrawerContent>
          <DrawerCloseButton />
          <DrawerHeader borderBottomWidth="1px">
            <Text noOfLines={1}>{conversation?.title?.trim() || 'Untitled'}</Text>
            <Text fontSize="xs" fontWeight={400} color="text-muted">
              Message moderation
            </Text>
          </DrawerHeader>
          <DrawerBody>
            {isLoading ? (
              <Center py={16}>
                <Spinner color="brand.500" />
              </Center>
            ) : isError ? (
              <Text color="text-muted" mt={6}>
                Couldn't load messages.
              </Text>
            ) : messages.length === 0 ? (
              <Text color="text-muted" mt={6}>
                No messages in this conversation.
              </Text>
            ) : (
              <VStack align="stretch" spacing={3} py={4}>
                {messages.map((m) => (
                  <Flex
                    key={m.id}
                    borderWidth="1px"
                    borderColor="panel-border"
                    borderRadius="lg"
                    px={3}
                    py={2}
                    gap={2}
                    align="start"
                    opacity={m.deleted ? 0.6 : 1}
                  >
                    <Box minW={0} flex={1}>
                      <HStack spacing={2} mb={1}>
                        <Text fontWeight={600} fontSize="sm" noOfLines={1}>
                          {m.senderUsername}
                        </Text>
                        <Text fontSize="xs" color="text-muted" flexShrink={0}>
                          {formatDayLabel(m.createdAt)} · {formatTime(m.createdAt)}
                        </Text>
                        {m.editedAt && (
                          <Badge fontSize="0.6rem" variant="subtle">
                            edited
                          </Badge>
                        )}
                      </HStack>
                      {m.deleted ? (
                        <Text fontSize="sm" fontStyle="italic" color="text-muted">
                          Message deleted
                        </Text>
                      ) : (
                        <Text fontSize="sm" whiteSpace="pre-wrap" wordBreak="break-word">
                          {m.body}
                        </Text>
                      )}
                    </Box>
                    {!m.deleted && (
                      <IconButton
                        aria-label="Delete message"
                        icon={<FiTrash2 />}
                        size="sm"
                        variant="ghost"
                        colorScheme="red"
                        onClick={() => setTarget(m)}
                      />
                    )}
                  </Flex>
                ))}
              </VStack>
            )}
          </DrawerBody>
        </DrawerContent>
      </Drawer>

      <AlertDialog
        isOpen={target !== null}
        leastDestructiveRef={cancelRef}
        onClose={() => setTarget(null)}
        isCentered
      >
        <AlertDialogOverlay>
          <AlertDialogContent borderRadius="xl">
            <AlertDialogHeader fontSize="lg" fontWeight="bold">
              Delete message
            </AlertDialogHeader>
            <AlertDialogBody>
              This soft-deletes the message for all members and cannot be undone.
            </AlertDialogBody>
            <AlertDialogFooter gap={2}>
              <Button ref={cancelRef} variant="ghost" onClick={() => setTarget(null)}>
                Cancel
              </Button>
              <Button colorScheme="red" onClick={confirmDelete} isLoading={deleteMessage.isPending}>
                Delete
              </Button>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialogOverlay>
      </AlertDialog>
    </>
  );
}
