import { useEffect, useRef, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Center,
  Flex,
  Heading,
  HStack,
  IconButton,
  Input,
  InputGroup,
  InputLeftElement,
  Select,
  Spacer,
  Spinner,
  Table,
  TableContainer,
  Tbody,
  Td,
  Text,
  Th,
  Thead,
  Tooltip,
  Tr,
  useDisclosure,
  AlertDialog,
  AlertDialogBody,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogOverlay,
} from '@chakra-ui/react';
import {
  FiChevronLeft,
  FiChevronRight,
  FiEye,
  FiMessageSquare,
  FiRotateCcw,
  FiSearch,
  FiTrash2,
} from 'react-icons/fi';
import { AdminHeader } from './AdminHeader';
import { ConversationMessagesDrawer } from './ConversationMessagesDrawer';
import { EmptyState } from '../../components/EmptyState';
import {
  useAdminConversations,
  useAdminConversationMutations,
} from '../../hooks/useAdminConversations';
import { formatDayLabel } from '../../utils/formatters';
import type { AdminConversationDto, ConversationType } from '../../types/dto';

const PAGE_SIZE = 20;

type TypeFilter = '' | ConversationType;
type StatusFilter = '' | 'active' | 'deleted';

export function AdminConversationsPage() {
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('');
  const [page, setPage] = useState(0);
  const [target, setTarget] = useState<AdminConversationDto | null>(null);
  const [viewing, setViewing] = useState<AdminConversationDto | null>(null);

  const confirm = useDisclosure();
  const cancelRef = useRef<HTMLButtonElement>(null);
  const { deleteConversation, restoreConversation } = useAdminConversationMutations();

  useEffect(() => {
    const handle = setTimeout(() => {
      setDebouncedSearch(search.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(handle);
  }, [search]);

  useEffect(() => {
    setPage(0);
  }, [typeFilter, statusFilter]);

  const deletedFilter =
    statusFilter === 'deleted' ? true : statusFilter === 'active' ? false : undefined;

  const { data, isLoading, isError, isFetching } = useAdminConversations({
    q: debouncedSearch,
    type: typeFilter || undefined,
    deleted: deletedFilter,
    page,
    size: PAGE_SIZE,
  });

  const rows = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const canPrev = page > 0;
  const canNext = page + 1 < totalPages;

  function askDelete(conversation: AdminConversationDto) {
    setTarget(conversation);
    confirm.onOpen();
  }

  function handleDelete() {
    if (!target) return;
    deleteConversation.mutate(target.id, {
      onSuccess: () => {
        confirm.onClose();
        setTarget(null);
      },
    });
  }

  function handleRestore(conversation: AdminConversationDto) {
    restoreConversation.mutate(conversation.id);
  }

  return (
    <Flex direction="column" h="100vh" overflow="hidden">
      <AdminHeader />

      <Box flex={1} overflowY="auto" px={{ base: 4, md: 8 }} py={6}>
        <Box maxW="1100px" mx="auto">
          <Flex
            align={{ base: 'start', md: 'center' }}
            mb={6}
            gap={3}
            direction={{ base: 'column', md: 'row' }}
          >
            <Box>
              <Heading size="lg">Conversations</Heading>
              <Text color="text-muted" fontSize="sm">
                {data
                  ? `${totalElements} ${totalElements === 1 ? 'conversation' : 'conversations'}`
                  : 'Oversee all conversations'}
              </Text>
            </Box>
            <Spacer />
            <HStack w={{ base: 'full', md: 'auto' }} flexWrap="wrap">
              <InputGroup maxW={{ base: 'full', md: '280px' }}>
                <InputLeftElement pointerEvents="none">
                  <FiSearch />
                </InputLeftElement>
                <Input
                  placeholder="Search by title"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </InputGroup>
              <Select
                maxW={{ base: 'full', md: '150px' }}
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value as TypeFilter)}
              >
                <option value="">All types</option>
                <option value="GROUP">Group</option>
                <option value="DIRECT">Direct</option>
              </Select>
              <Select
                maxW={{ base: 'full', md: '150px' }}
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
              >
                <option value="">All statuses</option>
                <option value="active">Active</option>
                <option value="deleted">Deleted</option>
              </Select>
            </HStack>
          </Flex>

          {isLoading ? (
            <Center py={20}>
              <Spinner size="lg" color="brand.500" />
            </Center>
          ) : isError ? (
            <EmptyState
              icon={FiMessageSquare}
              title="Couldn't load conversations"
              description="Something went wrong. Try again shortly."
            />
          ) : rows.length === 0 ? (
            <EmptyState
              icon={FiMessageSquare}
              title="No conversations found"
              description={
                debouncedSearch ? 'No conversations match your search.' : 'Nothing here yet.'
              }
            />
          ) : (
            <>
              <TableContainer
                borderWidth="1px"
                borderColor="panel-border"
                borderRadius="xl"
                overflow="hidden"
              >
                <Table size="sm" variant="simple">
                  <Thead bg="app-bg">
                    <Tr>
                      <Th>Title</Th>
                      <Th>Type</Th>
                      <Th isNumeric>Members</Th>
                      <Th isNumeric>Messages</Th>
                      <Th>Created</Th>
                      <Th>Status</Th>
                      <Th textAlign="right">Actions</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {rows.map((c) => (
                      <Tr key={c.id}>
                        <Td fontWeight={600} maxW="240px">
                          <Text noOfLines={1}>{c.title?.trim() || 'Untitled'}</Text>
                        </Td>
                        <Td>
                          <Badge colorScheme={c.type === 'GROUP' ? 'brand' : 'gray'}>
                            {c.type}
                          </Badge>
                        </Td>
                        <Td isNumeric>{c.memberCount}</Td>
                        <Td isNumeric>{c.messageCount}</Td>
                        <Td whiteSpace="nowrap">{formatDayLabel(c.createdAt)}</Td>
                        <Td>
                          {c.deleted ? (
                            <Badge colorScheme="red">Deleted</Badge>
                          ) : (
                            <Badge colorScheme="green">Active</Badge>
                          )}
                        </Td>
                        <Td textAlign="right">
                          <HStack spacing={1} justify="flex-end">
                            <Tooltip label="View messages">
                              <IconButton
                                aria-label="View messages"
                                icon={<FiEye />}
                                size="sm"
                                variant="ghost"
                                onClick={() => setViewing(c)}
                              />
                            </Tooltip>
                            {c.deleted ? (
                              <Tooltip label="Restore conversation">
                                <IconButton
                                  aria-label="Restore conversation"
                                  icon={<FiRotateCcw />}
                                  size="sm"
                                  variant="ghost"
                                  colorScheme="green"
                                  isLoading={
                                    restoreConversation.isPending &&
                                    restoreConversation.variables === c.id
                                  }
                                  onClick={() => handleRestore(c)}
                                />
                              </Tooltip>
                            ) : (
                              <Tooltip label="Delete conversation">
                                <IconButton
                                  aria-label="Delete conversation"
                                  icon={<FiTrash2 />}
                                  size="sm"
                                  variant="ghost"
                                  colorScheme="red"
                                  onClick={() => askDelete(c)}
                                />
                              </Tooltip>
                            )}
                          </HStack>
                        </Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
              </TableContainer>

              <Flex mt={4} align="center" gap={3}>
                <Text color="text-muted" fontSize="sm">
                  Page {page + 1} of {Math.max(totalPages, 1)}
                </Text>
                {isFetching && <Spinner size="sm" color="brand.500" />}
                <Spacer />
                <HStack>
                  <Button
                    leftIcon={<FiChevronLeft />}
                    size="sm"
                    variant="outline"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    isDisabled={!canPrev}
                  >
                    Prev
                  </Button>
                  <Button
                    rightIcon={<FiChevronRight />}
                    size="sm"
                    variant="outline"
                    onClick={() => setPage((p) => p + 1)}
                    isDisabled={!canNext}
                  >
                    Next
                  </Button>
                </HStack>
              </Flex>
            </>
          )}
        </Box>
      </Box>

      <AlertDialog
        isOpen={confirm.isOpen}
        leastDestructiveRef={cancelRef}
        onClose={confirm.onClose}
        isCentered
      >
        <AlertDialogOverlay>
          <AlertDialogContent borderRadius="xl">
            <AlertDialogHeader fontSize="lg" fontWeight="bold">
              Delete conversation
            </AlertDialogHeader>
            <AlertDialogBody>
              This soft-deletes “{target?.title?.trim() || 'Untitled'}” for all members. This cannot
              be undone.
            </AlertDialogBody>
            <AlertDialogFooter>
              <Button ref={cancelRef} onClick={confirm.onClose} variant="ghost">
                Cancel
              </Button>
              <Button
                colorScheme="red"
                ml={3}
                onClick={handleDelete}
                isLoading={deleteConversation.isPending}
              >
                Delete
              </Button>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialogOverlay>
      </AlertDialog>

      <ConversationMessagesDrawer conversation={viewing} onClose={() => setViewing(null)} />
    </Flex>
  );
}
