import {
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
  Spinner,
  Text,
  useToast,
  VStack,
} from '@chakra-ui/react';
import { FiLogOut } from 'react-icons/fi';
import { format } from 'date-fns';
import { Avatar } from '../../components/Avatar';
import { PresenceBadge } from '../../components/PresenceBadge';
import { useAdminUserDetail, useAdminUserMutations } from '../../hooks/useAdminUsers';

function errorMessage(error: unknown, fallback: string): string {
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
  return message ?? fallback;
}

interface Props {
  userId: string | null;
  onClose: () => void;
}

export function UserDetailDrawer({ userId, onClose }: Props) {
  const toast = useToast();
  const { data, isLoading, isError } = useAdminUserDetail(userId);
  const { revokeSessions } = useAdminUserMutations();

  const handleRevoke = () => {
    if (!userId) return;
    revokeSessions.mutate(userId, {
      onSuccess: () =>
        toast({ status: 'success', title: 'Sessions revoked — user must sign in again', duration: 3000 }),
      onError: (e) =>
        toast({ status: 'error', title: errorMessage(e, 'Could not revoke sessions'), duration: 4000 }),
    });
  };

  return (
    <Drawer isOpen={Boolean(userId)} placement="right" onClose={onClose} size="md">
      <DrawerOverlay />
      <DrawerContent>
        <DrawerCloseButton />
        <DrawerHeader borderBottomWidth="1px">User details</DrawerHeader>
        <DrawerBody>
          {isLoading ? (
            <Center py={16}>
              <Spinner color="brand.500" />
            </Center>
          ) : isError || !data ? (
            <Text color="text-muted" mt={6}>
              Couldn't load this user.
            </Text>
          ) : (
            <VStack align="stretch" spacing={6} pt={4}>
              <HStack spacing={4}>
                <Box position="relative">
                  <Avatar name={data.user.displayName} size="lg" />
                  <Box position="absolute" bottom="0" right="0">
                    <PresenceBadge online={data.user.online} size="12px" />
                  </Box>
                </Box>
                <Box>
                  <Text fontWeight={700} fontSize="lg">
                    {data.user.displayName}
                  </Text>
                  <Text fontSize="sm" color="text-muted">
                    @{data.user.username}
                  </Text>
                  <HStack mt={2} spacing={2}>
                    <Badge colorScheme={data.user.role === 'ADMIN' ? 'purple' : 'gray'}>
                      {data.user.role}
                    </Badge>
                    <Badge colorScheme={data.user.active ? 'green' : 'red'}>
                      {data.user.active ? 'Active' : 'Inactive'}
                    </Badge>
                    <Badge colorScheme={data.user.online ? 'green' : 'gray'} variant="subtle">
                      {data.user.online ? 'Online' : 'Offline'}
                    </Badge>
                  </HStack>
                </Box>
              </HStack>

              <Box>
                <Text fontSize="sm" color="text-muted">
                  Joined {format(new Date(data.user.createdAt), 'MMM d, yyyy')} ·{' '}
                  {data.user.conversationCount} conversation
                  {data.user.conversationCount === 1 ? '' : 's'}
                </Text>
              </Box>

              <Button
                leftIcon={<FiLogOut />}
                variant="outline"
                colorScheme="orange"
                onClick={handleRevoke}
                isLoading={revokeSessions.isPending}
                alignSelf="flex-start"
              >
                Force logout (revoke sessions)
              </Button>

              <Box>
                <Text fontWeight={600} mb={2}>
                  Conversations ({data.conversations.length})
                </Text>
                {data.conversations.length === 0 ? (
                  <Text fontSize="sm" color="text-muted">
                    Not a member of any conversation.
                  </Text>
                ) : (
                  <VStack align="stretch" spacing={2}>
                    {data.conversations.map((c) => (
                      <Flex
                        key={c.id}
                        align="center"
                        justify="space-between"
                        borderWidth="1px"
                        borderColor="panel-border"
                        borderRadius="lg"
                        px={3}
                        py={2}
                      >
                        <Box minW={0}>
                          <Text noOfLines={1} fontWeight={500}>
                            {c.title?.trim() || 'Untitled'}
                          </Text>
                          <Text fontSize="xs" color="text-muted">
                            {c.memberCount} members · {c.messageCount} messages
                          </Text>
                        </Box>
                        <HStack spacing={2} flexShrink={0}>
                          <Badge colorScheme={c.type === 'GROUP' ? 'brand' : 'gray'}>{c.type}</Badge>
                          {c.deleted && <Badge colorScheme="red">Deleted</Badge>}
                        </HStack>
                      </Flex>
                    ))}
                  </VStack>
                )}
              </Box>
            </VStack>
          )}
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  );
}
