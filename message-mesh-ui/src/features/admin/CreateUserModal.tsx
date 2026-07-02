import { useState } from 'react';
import {
  Button,
  FormControl,
  FormErrorMessage,
  FormLabel,
  Input,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Select,
  useToast,
  VStack,
} from '@chakra-ui/react';
import { useAdminUserMutations } from '../../hooks/useAdminUsers';
import type { UserRole } from '../../types/dto';

function errorMessage(error: unknown, fallback: string): string {
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
  return message ?? fallback;
}

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateUserModal({ isOpen, onClose }: Props) {
  const toast = useToast();
  const { createUser } = useAdminUserMutations();

  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<UserRole>('USER');
  const [touched, setTouched] = useState(false);

  const usernameInvalid = username.trim().length < 3;
  const displayNameInvalid = displayName.trim().length === 0;
  const passwordInvalid = password.length < 6;
  const invalid = usernameInvalid || displayNameInvalid || passwordInvalid;

  const close = () => {
    setUsername('');
    setDisplayName('');
    setPassword('');
    setRole('USER');
    setTouched(false);
    onClose();
  };

  const submit = () => {
    setTouched(true);
    if (invalid) return;
    createUser.mutate(
      { username: username.trim(), displayName: displayName.trim(), password, role },
      {
        onSuccess: () => {
          toast({ status: 'success', title: `User @${username.trim()} created`, duration: 2500 });
          close();
        },
        onError: (e) =>
          toast({ status: 'error', title: errorMessage(e, 'Could not create user'), duration: 4000 }),
      },
    );
  };

  return (
    <Modal isOpen={isOpen} onClose={close} isCentered>
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>Add user</ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <VStack spacing={4} align="stretch">
            <FormControl isInvalid={touched && usernameInvalid}>
              <FormLabel>Username</FormLabel>
              <Input
                value={username}
                autoComplete="off"
                placeholder="e.g. jdoe"
                onChange={(e) => setUsername(e.target.value)}
              />
              <FormErrorMessage>Username must be at least 3 characters.</FormErrorMessage>
            </FormControl>

            <FormControl isInvalid={touched && displayNameInvalid}>
              <FormLabel>Display name</FormLabel>
              <Input
                value={displayName}
                placeholder="e.g. Jane Doe"
                onChange={(e) => setDisplayName(e.target.value)}
              />
              <FormErrorMessage>Display name is required.</FormErrorMessage>
            </FormControl>

            <FormControl isInvalid={touched && passwordInvalid}>
              <FormLabel>Temporary password</FormLabel>
              <Input
                type="password"
                value={password}
                autoComplete="new-password"
                placeholder="At least 6 characters"
                onChange={(e) => setPassword(e.target.value)}
              />
              <FormErrorMessage>Password must be at least 6 characters.</FormErrorMessage>
            </FormControl>

            <FormControl>
              <FormLabel>Role</FormLabel>
              <Select value={role} onChange={(e) => setRole(e.target.value as UserRole)}>
                <option value="USER">User</option>
                <option value="ADMIN">Admin</option>
              </Select>
            </FormControl>
          </VStack>
        </ModalBody>
        <ModalFooter gap={2}>
          <Button variant="ghost" onClick={close}>
            Cancel
          </Button>
          <Button colorScheme="brand" onClick={submit} isLoading={createUser.isPending}>
            Create user
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}
