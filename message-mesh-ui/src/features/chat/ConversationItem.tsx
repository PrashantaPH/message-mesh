import { Badge, HStack, Text, VStack } from '@chakra-ui/react';
import { Avatar } from '../../components/Avatar';
import { formatTime } from '../../utils/formatters';
import type { ConversationDto } from '../../types/dto';

interface Props {
  conversation: ConversationDto;
  isActive: boolean;
  onClick: () => void;
}

export function ConversationItem({ conversation, isActive, onClick }: Props) {
  const title = conversation.title?.trim() || 'Direct message';
  const last = conversation.lastMessage;

  return (
    <HStack
      px={3}
      py={3}
      spacing={3}
      cursor="pointer"
      borderRadius="lg"
      onClick={onClick}
      bg={isActive ? 'brand.500' : 'transparent'}
      color={isActive ? 'white' : 'inherit'}
      _hover={{ bg: isActive ? 'brand.500' : 'blackAlpha.50', _dark: { bg: isActive ? 'brand.500' : 'whiteAlpha.100' } }}
      transition="background 0.15s"
      align="center"
    >
      <Avatar name={title} size="md" isGroup={conversation.type === 'GROUP'} />
      <VStack align="stretch" spacing={0} flex={1} minW={0}>
        <HStack justify="space-between" spacing={2}>
          <Text fontWeight={600} noOfLines={1}>
            {title}
          </Text>
          {last && (
            <Text fontSize="xs" opacity={0.8} flexShrink={0}>
              {formatTime(last.createdAt)}
            </Text>
          )}
        </HStack>
        <HStack justify="space-between" spacing={2}>
          <Text fontSize="sm" noOfLines={1} opacity={isActive ? 0.9 : 0.65}>
            {last ? `${last.senderUsername}: ${last.body}` : 'No messages yet'}
          </Text>
          {conversation.unreadCount > 0 && (
            <Badge
              colorScheme={isActive ? 'whiteAlpha' : 'brand'}
              borderRadius="full"
              px={2}
              flexShrink={0}
            >
              {conversation.unreadCount}
            </Badge>
          )}
        </HStack>
      </VStack>
    </HStack>
  );
}
