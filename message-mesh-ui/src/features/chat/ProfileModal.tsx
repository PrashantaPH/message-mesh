import { useEffect, useRef, useState } from 'react';
import {
  AlertDialog,
  AlertDialogBody,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogOverlay,
  Button,
  Divider,
  FormControl,
  FormLabel,
  Heading,
  Input,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalHeader,
  ModalOverlay,
  Stack,
  Text,
  useDisclosure,
} from '@chakra-ui/react';
import { useProfile } from '../../hooks/useProfile';
import { useAuth } from '../../hooks/useAuth';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

export function ProfileModal({ isOpen, onClose }: Props) {
  const { user, updateProfile, changePassword, deleteAccount } = useProfile();
  const { logout } = useAuth();

  const [displayName, setDisplayName] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');

  const confirmDelete = useDisclosure();
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (isOpen) setDisplayName(user?.displayName ?? '');
  }, [isOpen, user?.displayName]);

  function saveName() {
    const next = displayName.trim();
    if (!next || next === user?.displayName) return;
    updateProfile.mutate(next);
  }

  function savePassword() {
    if (!currentPassword || newPassword.length < 6) return;
    changePassword.mutate(
      { currentPassword, newPassword },
      {
        onSuccess: () => {
          setCurrentPassword('');
          setNewPassword('');
        },
      },
    );
  }

  function handleDelete() {
    deleteAccount.mutate(undefined, {
      onSuccess: () => {
        confirmDelete.onClose();
        onClose();
        logout();
      },
    });
  }

  return (
    <>
      <Modal isOpen={isOpen} onClose={onClose} isCentered size="md">
        <ModalOverlay />
        <ModalContent borderRadius="xl">
          <ModalHeader>Your profile</ModalHeader>
          <ModalCloseButton />
          <ModalBody pb={6}>
            <Stack spacing={5}>
              <Stack spacing={3}>
                <Heading size="xs" color="text-muted" textTransform="uppercase" letterSpacing="wide">
                  Display name
                </Heading>
                <FormControl>
                  <Input
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    maxLength={128}
                  />
                </FormControl>
                <Button
                  size="sm"
                  alignSelf="flex-start"
                  onClick={saveName}
                  isDisabled={!displayName.trim() || displayName.trim() === user?.displayName}
                  isLoading={updateProfile.isPending}
                >
                  Save name
                </Button>
              </Stack>

              <Divider />

              <Stack spacing={3}>
                <Heading size="xs" color="text-muted" textTransform="uppercase" letterSpacing="wide">
                  Change password
                </Heading>
                <FormControl>
                  <FormLabel fontSize="sm">Current password</FormLabel>
                  <Input
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    autoComplete="current-password"
                  />
                </FormControl>
                <FormControl>
                  <FormLabel fontSize="sm">New password</FormLabel>
                  <Input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    autoComplete="new-password"
                  />
                  <Text fontSize="xs" color="text-muted" mt={1}>
                    At least 6 characters.
                  </Text>
                </FormControl>
                <Button
                  size="sm"
                  alignSelf="flex-start"
                  onClick={savePassword}
                  isDisabled={!currentPassword || newPassword.length < 6}
                  isLoading={changePassword.isPending}
                >
                  Update password
                </Button>
              </Stack>

              <Divider />

              <Stack spacing={2}>
                <Heading size="xs" color="red.400" textTransform="uppercase" letterSpacing="wide">
                  Danger zone
                </Heading>
                <Text fontSize="sm" color="text-muted">
                  Permanently delete your account and remove you from all conversations.
                </Text>
                <Button
                  size="sm"
                  colorScheme="red"
                  variant="outline"
                  alignSelf="flex-start"
                  onClick={confirmDelete.onOpen}
                >
                  Delete account
                </Button>
              </Stack>
            </Stack>
          </ModalBody>
        </ModalContent>
      </Modal>

      <AlertDialog
        isOpen={confirmDelete.isOpen}
        leastDestructiveRef={cancelRef}
        onClose={confirmDelete.onClose}
        isCentered
      >
        <AlertDialogOverlay>
          <AlertDialogContent borderRadius="xl">
            <AlertDialogHeader fontSize="lg" fontWeight="bold">
              Delete account
            </AlertDialogHeader>
            <AlertDialogBody>
              This cannot be undone. You will be signed out immediately.
            </AlertDialogBody>
            <AlertDialogFooter>
              <Button ref={cancelRef} onClick={confirmDelete.onClose} variant="ghost">
                Cancel
              </Button>
              <Button
                colorScheme="red"
                ml={3}
                onClick={handleDelete}
                isLoading={deleteAccount.isPending}
              >
                Delete
              </Button>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialogOverlay>
      </AlertDialog>
    </>
  );
}
