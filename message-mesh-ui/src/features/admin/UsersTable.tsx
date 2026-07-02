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
  Checkbox,
  FormControl,
  FormErrorMessage,
  FormLabel,
  HStack,
  IconButton,
  Input,
  Menu,
  MenuButton,
  MenuDivider,
  MenuItem,
  MenuList,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Table,
  TableContainer,
  Tbody,
  Td,
  Text,
  Th,
  Thead,
  Tr,
  VStack,
  useColorModeValue,
  useToast,
} from '@chakra-ui/react';
import { format } from 'date-fns';
import {
  FiInfo,
  FiKey,
  FiLogOut,
  FiMoreVertical,
  FiShield,
  FiShieldOff,
  FiTrash2,
  FiUserCheck,
  FiUserX,
} from 'react-icons/fi';
import { Avatar } from '../../components/Avatar';
import { PresenceBadge } from '../../components/PresenceBadge';
import { useAdminUserMutations } from '../../hooks/useAdminUsers';
import type { AdminUserDto } from '../../types/dto';

function errorMessage(error: unknown, fallback: string): string {
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
  return message ?? fallback;
}

interface Props {
  users: AdminUserDto[];
  currentUsername: string;
  selected: Set<string>;
  onToggle: (id: string) => void;
  onToggleAll: () => void;
  allSelected: boolean;
  someSelected: boolean;
  onOpenDetail: (id: string) => void;
}

type PendingAction = { kind: 'role' | 'deactivate'; user: AdminUserDto } | null;

export function UsersTable({
  users,
  currentUsername,
  selected,
  onToggle,
  onToggleAll,
  allSelected,
  someSelected,
  onOpenDetail,
}: Props) {
  const toast = useToast();
  const { updateRole, updateStatus, revokeSessions, deleteUser } = useAdminUserMutations();
  const rowHover = useColorModeValue('gray.50', 'whiteAlpha.100');

  const [resetTarget, setResetTarget] = useState<AdminUserDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminUserDto | null>(null);
  const [pending, setPending] = useState<PendingAction>(null);

  const applyRole = (user: AdminUserDto) => {
    const role = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    updateRole.mutate(
      { id: user.id, role },
      {
        onSuccess: () =>
          toast({ status: 'success', title: `${user.username} is now ${role}`, duration: 2500 }),
        onError: (e) =>
          toast({ status: 'error', title: errorMessage(e, 'Could not change role'), duration: 4000 }),
      },
    );
  };

  const applyStatus = (user: AdminUserDto, active: boolean) => {
    updateStatus.mutate(
      { id: user.id, active },
      {
        onSuccess: () =>
          toast({
            status: 'success',
            title: `${user.username} ${active ? 'activated' : 'deactivated'}`,
            duration: 2500,
          }),
        onError: (e) =>
          toast({ status: 'error', title: errorMessage(e, 'Could not update status'), duration: 4000 }),
      },
    );
  };

  const handleStatusMenu = (user: AdminUserDto) => {
    if (user.active) {
      setPending({ kind: 'deactivate', user });
    } else {
      applyStatus(user, true);
    }
  };

  const handleRevoke = (user: AdminUserDto) => {
    revokeSessions.mutate(user.id, {
      onSuccess: () =>
        toast({ status: 'success', title: `Sessions revoked for ${user.username}`, duration: 2500 }),
      onError: (e) =>
        toast({ status: 'error', title: errorMessage(e, 'Could not revoke sessions'), duration: 4000 }),
    });
  };

  const confirmPending = () => {
    if (!pending) return;
    if (pending.kind === 'role') applyRole(pending.user);
    else applyStatus(pending.user, false);
    setPending(null);
  };

  return (
    <>
      <TableContainer
        borderWidth="1px"
        borderColor="panel-border"
        borderRadius="lg"
        bg="panel-bg"
        overflowX="auto"
      >
        <Table variant="simple" size="md">
          <Thead>
            <Tr>
              <Th w="1%">
                <Checkbox
                  isChecked={allSelected}
                  isIndeterminate={someSelected && !allSelected}
                  onChange={onToggleAll}
                  aria-label="Select all users"
                />
              </Th>
              <Th>User</Th>
              <Th>Role</Th>
              <Th>Status</Th>
              <Th isNumeric>Conversations</Th>
              <Th>Joined</Th>
              <Th textAlign="right">Actions</Th>
            </Tr>
          </Thead>
          <Tbody>
            {users.map((user) => {
              const isSelf = user.username === currentUsername;
              return (
                <Tr key={user.id} _hover={{ bg: rowHover }}>
                  <Td>
                    <Checkbox
                      isChecked={selected.has(user.id)}
                      isDisabled={isSelf}
                      onChange={() => onToggle(user.id)}
                      aria-label={`Select ${user.username}`}
                    />
                  </Td>
                  <Td>
                    <HStack
                      spacing={3}
                      cursor="pointer"
                      onClick={() => onOpenDetail(user.id)}
                      role="button"
                    >
                      <Box position="relative">
                        <Avatar name={user.displayName} size="sm" />
                        <Box position="absolute" bottom="-1px" right="-1px">
                          <PresenceBadge online={user.online} size="10px" />
                        </Box>
                      </Box>
                      <VStack align="start" spacing={0}>
                        <Text fontWeight={600}>
                          {user.displayName}
                          {isSelf && (
                            <Text as="span" color="text-muted" fontWeight={400}>
                              {' '}
                              (you)
                            </Text>
                          )}
                        </Text>
                        <Text fontSize="xs" color="text-muted">
                          @{user.username}
                        </Text>
                      </VStack>
                    </HStack>
                  </Td>
                  <Td>
                    <Badge colorScheme={user.role === 'ADMIN' ? 'purple' : 'gray'}>
                      {user.role}
                    </Badge>
                  </Td>
                  <Td>
                    <Badge colorScheme={user.active ? 'green' : 'red'}>
                      {user.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </Td>
                  <Td isNumeric>{user.conversationCount}</Td>
                  <Td>
                    <Text fontSize="sm" color="text-muted">
                      {format(new Date(user.createdAt), 'MMM d, yyyy')}
                    </Text>
                  </Td>
                  <Td textAlign="right">
                    <Menu placement="bottom-end">
                      <MenuButton
                        as={IconButton}
                        aria-label={`Actions for ${user.username}`}
                        icon={<FiMoreVertical />}
                        variant="ghost"
                        size="sm"
                      />
                      <MenuList>
                        <MenuItem icon={<FiInfo />} onClick={() => onOpenDetail(user.id)}>
                          View details
                        </MenuItem>
                        <MenuDivider />
                        {user.role === 'ADMIN' ? (
                          <MenuItem
                            icon={<FiShieldOff />}
                            isDisabled={isSelf}
                            onClick={() => setPending({ kind: 'role', user })}
                          >
                            Demote to User
                          </MenuItem>
                        ) : (
                          <MenuItem
                            icon={<FiShield />}
                            onClick={() => setPending({ kind: 'role', user })}
                          >
                            Promote to Admin
                          </MenuItem>
                        )}
                        <MenuItem
                          icon={user.active ? <FiUserX /> : <FiUserCheck />}
                          isDisabled={isSelf && user.active}
                          onClick={() => handleStatusMenu(user)}
                        >
                          {user.active ? 'Deactivate' : 'Activate'}
                        </MenuItem>
                        <MenuItem icon={<FiLogOut />} onClick={() => handleRevoke(user)}>
                          Revoke sessions
                        </MenuItem>
                        <MenuItem icon={<FiKey />} onClick={() => setResetTarget(user)}>
                          Reset password
                        </MenuItem>
                        <MenuDivider />
                        <MenuItem
                          icon={<FiTrash2 />}
                          color="red.400"
                          isDisabled={isSelf}
                          onClick={() => setDeleteTarget(user)}
                        >
                          Delete user
                        </MenuItem>
                      </MenuList>
                    </Menu>
                  </Td>
                </Tr>
              );
            })}
          </Tbody>
        </Table>
      </TableContainer>

      <ConfirmActionDialog
        pending={pending}
        onClose={() => setPending(null)}
        onConfirm={confirmPending}
        isLoading={updateRole.isPending || updateStatus.isPending}
      />

      <ResetPasswordModal target={resetTarget} onClose={() => setResetTarget(null)} />
      <DeleteUserDialog
        target={deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={(user) =>
          deleteUser.mutate(user.id, {
            onSuccess: () => {
              toast({ status: 'success', title: `${user.username} deleted`, duration: 2500 });
              setDeleteTarget(null);
            },
            onError: (e) =>
              toast({ status: 'error', title: errorMessage(e, 'Could not delete user'), duration: 4000 }),
          })
        }
        isDeleting={deleteUser.isPending}
      />
    </>
  );
}

function ResetPasswordModal({
  target,
  onClose,
}: {
  target: AdminUserDto | null;
  onClose: () => void;
}) {
  const toast = useToast();
  const { resetPassword } = useAdminUserMutations();
  const [password, setPassword] = useState('');
  const [touched, setTouched] = useState(false);
  const isOpen = target !== null;
  const invalid = password.length < 6;

  const close = () => {
    setPassword('');
    setTouched(false);
    onClose();
  };

  const submit = () => {
    setTouched(true);
    if (invalid || !target) return;
    resetPassword.mutate(
      { id: target.id, newPassword: password },
      {
        onSuccess: () => {
          toast({ status: 'success', title: `Password reset for ${target.username}`, duration: 2500 });
          close();
        },
        onError: (e) =>
          toast({ status: 'error', title: errorMessage(e, 'Could not reset password'), duration: 4000 }),
      },
    );
  };

  return (
    <Modal isOpen={isOpen} onClose={close} isCentered>
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>Reset password{target ? ` — @${target.username}` : ''}</ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <FormControl isInvalid={touched && invalid}>
            <FormLabel>New password</FormLabel>
            <Input
              type="password"
              value={password}
              autoComplete="new-password"
              placeholder="At least 6 characters"
              onChange={(e) => setPassword(e.target.value)}
              onBlur={() => setTouched(true)}
            />
            <FormErrorMessage>Password must be at least 6 characters.</FormErrorMessage>
          </FormControl>
        </ModalBody>
        <ModalFooter gap={2}>
          <Button variant="ghost" onClick={close}>
            Cancel
          </Button>
          <Button colorScheme="brand" onClick={submit} isLoading={resetPassword.isPending}>
            Reset password
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}

function DeleteUserDialog({
  target,
  onClose,
  onConfirm,
  isDeleting,
}: {
  target: AdminUserDto | null;
  onClose: () => void;
  onConfirm: (user: AdminUserDto) => void;
  isDeleting: boolean;
}) {
  const cancelRef = useRef<HTMLButtonElement>(null);

  return (
    <AlertDialog
      isOpen={target !== null}
      leastDestructiveRef={cancelRef}
      onClose={onClose}
      isCentered
    >
      <AlertDialogOverlay>
        <AlertDialogContent>
          <AlertDialogHeader fontSize="lg" fontWeight="bold">
            Delete user
          </AlertDialogHeader>
          <AlertDialogBody>
            Permanently delete <strong>@{target?.username}</strong> and their conversation
            memberships? This cannot be undone. Their sent messages are retained.
          </AlertDialogBody>
          <AlertDialogFooter gap={2}>
            <Button ref={cancelRef} variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button
              colorScheme="red"
              isLoading={isDeleting}
              onClick={() => target && onConfirm(target)}
            >
              Delete
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialogOverlay>
    </AlertDialog>
  );
}

function ConfirmActionDialog({
  pending,
  onClose,
  onConfirm,
  isLoading,
}: {
  pending: PendingAction;
  onClose: () => void;
  onConfirm: () => void;
  isLoading: boolean;
}) {
  const cancelRef = useRef<HTMLButtonElement>(null);

  const isRole = pending?.kind === 'role';
  const nextRole = pending?.user.role === 'ADMIN' ? 'User' : 'Admin';
  const title = isRole ? 'Change role' : 'Deactivate user';
  const confirmLabel = isRole ? `Make ${nextRole}` : 'Deactivate';
  const colorScheme = isRole ? 'brand' : 'orange';

  return (
    <AlertDialog
      isOpen={pending !== null}
      leastDestructiveRef={cancelRef}
      onClose={onClose}
      isCentered
    >
      <AlertDialogOverlay>
        <AlertDialogContent>
          <AlertDialogHeader fontSize="lg" fontWeight="bold">
            {title}
          </AlertDialogHeader>
          <AlertDialogBody>
            {isRole ? (
              <>
                Change <strong>@{pending?.user.username}</strong> to <strong>{nextRole}</strong>?
              </>
            ) : (
              <>
                Deactivate <strong>@{pending?.user.username}</strong>? They will be signed out and
                unable to log in until reactivated.
              </>
            )}
          </AlertDialogBody>
          <AlertDialogFooter gap={2}>
            <Button ref={cancelRef} variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button colorScheme={colorScheme} isLoading={isLoading} onClick={onConfirm}>
              {confirmLabel}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialogOverlay>
    </AlertDialog>
  );
}
