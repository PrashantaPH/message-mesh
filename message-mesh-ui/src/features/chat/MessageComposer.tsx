import { useRef, type KeyboardEvent } from 'react';
import {
  Box,
  Flex,
  HStack,
  IconButton,
  Popover,
  PopoverBody,
  PopoverContent,
  PopoverTrigger,
  Portal,
  Stack,
  Text,
  Textarea,
  useColorMode,
  useDisclosure,
} from '@chakra-ui/react';
import { FiCornerUpLeft, FiSend, FiSmile, FiX } from 'react-icons/fi';
import EmojiPicker from '@emoji-mart/react';
import emojiData from '@emoji-mart/data';
import { useChatStore } from '../../store/chatStore';
import { useSendMessage } from '../../hooks/useSendMessage';
import { useSocket } from '../../ws/SocketContext';

const TYPING_THROTTLE_MS = 1500;

export function MessageComposer({ conversationId }: { conversationId: string }) {
  const draft = useChatStore((s) => s.drafts[conversationId] ?? '');
  const setDraft = useChatStore((s) => s.setDraft);
  const replyingTo = useChatStore((s) => s.replyByConv[conversationId] ?? null);
  const setReplyingTo = useChatStore((s) => s.setReplyingTo);
  const send = useSendMessage(conversationId);
  const { sendTyping } = useSocket();
  const lastTypingRef = useRef(0);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const { colorMode } = useColorMode();
  const emojiPopover = useDisclosure();

  function handleSend() {
    if (!draft.trim()) return;
    send(draft, replyingTo?.id ?? null);
    setDraft(conversationId, '');
    setReplyingTo(conversationId, null);
  }

  function insertEmoji(native: string) {
    const el = textareaRef.current;
    const start = el?.selectionStart ?? draft.length;
    const end = el?.selectionEnd ?? draft.length;
    const next = draft.slice(0, start) + native + draft.slice(end);
    setDraft(conversationId, next);
    // Restore focus and place the caret right after the inserted emoji.
    requestAnimationFrame(() => {
      const node = textareaRef.current;
      if (node) {
        const pos = start + native.length;
        node.focus();
        node.setSelectionRange(pos, pos);
      }
    });
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  function handleChange(value: string) {
    setDraft(conversationId, value);
    const now = Date.now();
    if (now - lastTypingRef.current > TYPING_THROTTLE_MS) {
      lastTypingRef.current = now;
      sendTyping({ conversationId });
    }
  }

  return (
    <Box borderTop="1px solid" borderColor="panel-border" bg="panel-bg" py={3} px={{ base: 3, md: 4 }}>
      <Box w="full" maxW="3xl" mx="auto">
        {replyingTo && (
          <HStack
            mb={2}
            px={3}
            py={2}
            bg="app-bg"
            borderRadius="lg"
            borderLeft="3px solid"
            borderColor="brand.400"
            align="center"
          >
            <FiCornerUpLeft />
            <Stack spacing={0} flex={1} minW={0}>
              <Text fontSize="xs" fontWeight={700} color="brand.400" noOfLines={1}>
                Replying to {replyingTo.senderUsername}
              </Text>
              <Text fontSize="xs" color="text-muted" noOfLines={1}>
                {replyingTo.deleted ? 'Message deleted' : replyingTo.body}
              </Text>
            </Stack>
            <IconButton
              aria-label="Cancel reply"
              icon={<FiX />}
              size="xs"
              variant="ghost"
              onClick={() => setReplyingTo(conversationId, null)}
            />
          </HStack>
        )}
        <Flex w="full" gap={2} align="flex-end">
          <Box position="relative" flex={1}>
            <Popover
              isOpen={emojiPopover.isOpen}
              onClose={emojiPopover.onClose}
              placement="top-start"
              isLazy
            >
              <PopoverTrigger>
                <IconButton
                  aria-label="Insert emoji"
                  icon={<FiSmile />}
                  variant="ghost"
                  borderRadius="full"
                  size="sm"
                  color="text-muted"
                  position="absolute"
                  left="6px"
                  bottom="6px"
                  zIndex={2}
                  onClick={emojiPopover.onToggle}
                />
              </PopoverTrigger>
              <Portal>
                <PopoverContent w="auto" bg="transparent" border="none" boxShadow="none" _focusVisible={{ boxShadow: 'none' }}>
                  <PopoverBody p={0}>
                    <EmojiPicker
                      data={emojiData}
                      theme={colorMode}
                      previewPosition="none"
                      navPosition="top"
                      onEmojiSelect={(emoji: { native: string }) => insertEmoji(emoji.native)}
                    />
                  </PopoverBody>
                </PopoverContent>
              </Portal>
            </Popover>
            <Textarea
              ref={textareaRef}
              value={draft}
              onChange={(e) => handleChange(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Type a message…  (Enter to send, Shift+Enter for a new line)"
              rows={1}
              resize="none"
              minH="44px"
              maxH="160px"
              pl="44px"
              borderRadius="2xl"
              bg="app-bg"
              border="none"
              _focusVisible={{ boxShadow: 'outline' }}
            />
          </Box>
          <IconButton
            aria-label="Send message"
            icon={<FiSend />}
            onClick={handleSend}
            isDisabled={!draft.trim()}
            borderRadius="full"
            boxSize="44px"
            minW="44px"
          />
        </Flex>
      </Box>
    </Box>
  );
}
