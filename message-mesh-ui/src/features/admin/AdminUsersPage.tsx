import { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Button,
  Center,
  Flex,
  Heading,
  HStack,
  Input,
  InputGroup,
  InputLeftElement,
  Select,
  Spacer,
  Spinner,
  Text,
  useDisclosure,
  useToast,
} from '@chakra-ui/react';
import {
  FiChevronLeft,
  FiChevronRight,
  FiDownload,
  FiSearch,
  FiTrash2,
  FiUserCheck,
  FiUserPlus,
  FiUsers,
  FiUserX,
} from 'react-icons/fi';
import { AdminHeader } from './AdminHeader';
import { CreateUserModal } from './CreateUserModal';
import { UserDetailDrawer } from './UserDetailDrawer';
import { EmptyState } from '../../components/EmptyState';
import { useAdminUsers, useAdminUserMutations } from '../../hooks/useAdminUsers';
import { useAuth } from '../../hooks/useAuth';
import { UsersTable } from './UsersTable';
import { downloadCsv } from '../../utils/csv';
import type { UserRole } from '../../types/dto';

type RoleFilter = '' | UserRole;
type StatusFilter = '' | 'active' | 'inactive';

export function AdminUsersPage() {
  const { user } = useAuth();
  const toast = useToast();
  const { updateStatus, deleteUser } = useAdminUserMutations();

  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<RoleFilter>('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('');
  const [pageSize, setPageSize] = useState(20);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [detailUserId, setDetailUserId] = useState<string | null>(null);

  const createModal = useDisclosure();

  // Debounce the search input and reset to the first page on a new query.
  useEffect(() => {
    const handle = setTimeout(() => {
      setDebouncedSearch(search.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(handle);
  }, [search]);

  // Reset paging + selection whenever the active filters change.
  useEffect(() => {
    setPage(0);
    setSelected(new Set());
  }, [roleFilter, statusFilter, pageSize, debouncedSearch]);

  const activeFilter =
    statusFilter === 'active' ? true : statusFilter === 'inactive' ? false : undefined;

  const { data, isLoading, isError, isFetching } = useAdminUsers({
    q: debouncedSearch,
    role: roleFilter || undefined,
    active: activeFilter,
    page,
    size: pageSize,
  });

  const users = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const canPrev = page > 0;
  const canNext = page + 1 < totalPages;

  const currentUsername = user?.username ?? '';

  // Only rows other than the signed-in admin can be bulk-selected.
  const selectableIds = useMemo(
    () => users.filter((u) => u.username !== currentUsername).map((u) => u.id),
    [users, currentUsername],
  );
  const allSelected = selectableIds.length > 0 && selectableIds.every((id) => selected.has(id));
  const someSelected = selectableIds.some((id) => selected.has(id));

  const onToggle = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const onToggleAll = () => {
    setSelected((prev) => {
      if (selectableIds.every((id) => prev.has(id))) return new Set();
      return new Set(selectableIds);
    });
  };

  const selectedUsers = useMemo(
    () => users.filter((u) => selected.has(u.id)),
    [users, selected],
  );

  const bulkSetActive = (active: boolean) => {
    const targets = selectedUsers.filter((u) => u.active !== active);
    if (targets.length === 0) return;
    Promise.allSettled(
      targets.map((u) => updateStatus.mutateAsync({ id: u.id, active })),
    ).then(() => {
      toast({
        status: 'success',
        title: `${targets.length} user(s) ${active ? 'activated' : 'deactivated'}`,
        duration: 2500,
      });
      setSelected(new Set());
    });
  };

  const bulkDelete = () => {
    const targets = selectedUsers;
    if (targets.length === 0) return;
    Promise.allSettled(targets.map((u) => deleteUser.mutateAsync(u.id))).then(() => {
      toast({ status: 'success', title: `${targets.length} user(s) deleted`, duration: 2500 });
      setSelected(new Set());
    });
  };

  const exportCsv = () => {
    downloadCsv(
      'users.csv',
      ['Username', 'Display name', 'Role', 'Status', 'Conversations', 'Online', 'Joined'],
      users.map((u) => [
        u.username,
        u.displayName,
        u.role,
        u.active ? 'Active' : 'Inactive',
        u.conversationCount,
        u.online ? 'Online' : 'Offline',
        new Date(u.createdAt).toLocaleString(),
      ]),
    );
  };

  const busy = updateStatus.isPending || deleteUser.isPending;

  return (
    <Flex direction="column" h="100vh" overflow="hidden">
      <AdminHeader />

      <Box flex={1} overflowY="auto" px={{ base: 4, md: 8 }} py={6}>
        <Box maxW="1100px" mx="auto">
          <Flex align={{ base: 'start', md: 'center' }} mb={4} gap={3} direction={{ base: 'column', md: 'row' }}>
            <Box>
              <Heading size="lg">Users</Heading>
              <Text color="text-muted" fontSize="sm">
                {data
                  ? `${totalElements} registered ${totalElements === 1 ? 'user' : 'users'}`
                  : 'Manage all accounts'}
              </Text>
            </Box>
            <Spacer />
            <HStack>
              <Button leftIcon={<FiDownload />} size="sm" variant="outline" onClick={exportCsv} isDisabled={users.length === 0}>
                Export CSV
              </Button>
              <Button leftIcon={<FiUserPlus />} size="sm" colorScheme="brand" onClick={createModal.onOpen}>
                Add user
              </Button>
            </HStack>
          </Flex>

          <Flex mb={6} gap={3} direction={{ base: 'column', md: 'row' }} align={{ base: 'stretch', md: 'center' }}>
            <InputGroup maxW={{ base: 'full', md: '320px' }}>
              <InputLeftElement pointerEvents="none">
                <FiSearch />
              </InputLeftElement>
              <Input
                placeholder="Search by name or username"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </InputGroup>
            <Select
              maxW={{ base: 'full', md: '160px' }}
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value as RoleFilter)}
            >
              <option value="">All roles</option>
              <option value="ADMIN">Admins</option>
              <option value="USER">Users</option>
            </Select>
            <Select
              maxW={{ base: 'full', md: '160px' }}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
            >
              <option value="">All statuses</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </Select>
            <Spacer display={{ base: 'none', md: 'block' }} />
            <Select
              maxW={{ base: 'full', md: '130px' }}
              value={pageSize}
              onChange={(e) => setPageSize(Number(e.target.value))}
            >
              <option value={10}>10 / page</option>
              <option value={20}>20 / page</option>
              <option value={50}>50 / page</option>
              <option value={100}>100 / page</option>
            </Select>
          </Flex>

          {selected.size > 0 && (
            <Flex
              mb={4}
              p={3}
              borderWidth="1px"
              borderRadius="lg"
              align="center"
              gap={3}
              bg="bg-subtle"
            >
              <Text fontSize="sm" fontWeight="medium">
                {selected.size} selected
              </Text>
              <Spacer />
              <HStack>
                <Button size="sm" leftIcon={<FiUserCheck />} variant="outline" onClick={() => bulkSetActive(true)} isLoading={busy}>
                  Activate
                </Button>
                <Button size="sm" leftIcon={<FiUserX />} variant="outline" onClick={() => bulkSetActive(false)} isLoading={busy}>
                  Deactivate
                </Button>
                <Button size="sm" leftIcon={<FiTrash2 />} colorScheme="red" variant="outline" onClick={bulkDelete} isLoading={busy}>
                  Delete
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setSelected(new Set())}>
                  Clear
                </Button>
              </HStack>
            </Flex>
          )}

          {isLoading ? (
            <Center py={20}>
              <Spinner size="lg" color="brand.500" />
            </Center>
          ) : isError ? (
            <EmptyState
              icon={FiUsers}
              title="Couldn't load users"
              description="Something went wrong fetching the user list. Try again shortly."
            />
          ) : users.length === 0 ? (
            <EmptyState
              icon={FiUsers}
              title="No users found"
              description={
                debouncedSearch || roleFilter || statusFilter
                  ? 'No users match your filters.'
                  : 'No users are registered yet.'
              }
            />
          ) : (
            <>
              <UsersTable
                users={users}
                currentUsername={currentUsername}
                selected={selected}
                onToggle={onToggle}
                onToggleAll={onToggleAll}
                allSelected={allSelected}
                someSelected={someSelected}
                onOpenDetail={setDetailUserId}
              />
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

      <CreateUserModal isOpen={createModal.isOpen} onClose={createModal.onClose} />
      <UserDetailDrawer userId={detailUserId} onClose={() => setDetailUserId(null)} />
    </Flex>
  );
}

