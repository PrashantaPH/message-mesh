import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useToast } from '@chakra-ui/react';
import { userApi } from '../api/user.api';
import { useAuthStore } from '../store/authStore';
import type { ChangePasswordRequest } from '../types/dto';

/**
 * Self-service profile mutations. Display-name updates refresh the persisted
 * auth user; a password change swaps in the freshly issued token so the session
 * stays alive.
 */
export function useProfile() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const setAuth = useAuthStore((s) => s.setAuth);
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);

  const updateProfile = useMutation({
    mutationFn: (displayName: string) => userApi.updateMe(displayName),
    onSuccess: (updated) => {
      if (token) setAuth(token, updated);
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      toast({ title: 'Profile updated', status: 'success', duration: 2500 });
    },
    onError: () =>
      toast({ title: 'Could not update profile', status: 'error', duration: 3000 }),
  });

  const changePassword = useMutation({
    mutationFn: (payload: ChangePasswordRequest) => userApi.changePassword(payload),
    onSuccess: (auth) => {
      setAuth(auth.token, auth.user);
      toast({ title: 'Password changed', status: 'success', duration: 2500 });
    },
    onError: () =>
      toast({
        title: 'Could not change password',
        description: 'Check that your current password is correct.',
        status: 'error',
        duration: 3500,
      }),
  });

  const deleteAccount = useMutation({
    mutationFn: () => userApi.deleteMe(),
    onError: () =>
      toast({
        title: 'Could not delete account',
        description: 'The last administrator cannot be deleted.',
        status: 'error',
        duration: 3500,
      }),
  });

  return { user, updateProfile, changePassword, deleteAccount };
}
