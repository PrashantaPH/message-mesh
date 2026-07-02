import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import { adminApi, type ListUsersParams } from '../api/admin.api';
import type { CreateUserRequest, UserRole } from '../types/dto';

const ADMIN_USERS_KEY = ['admin', 'users'] as const;

export function useAdminUsers(params: ListUsersParams = {}) {
  const { q = '', role, active, page = 0, size = 20 } = params;
  return useQuery({
    queryKey: [...ADMIN_USERS_KEY, { q, role: role ?? null, active: active ?? null, page, size }],
    queryFn: () => adminApi.listUsers({ q: q || undefined, role, active, page, size }),
    refetchOnWindowFocus: false,
    placeholderData: keepPreviousData,
  });
}

export function useAdminUserDetail(id: string | null) {
  return useQuery({
    queryKey: [...ADMIN_USERS_KEY, 'detail', id],
    queryFn: () => adminApi.getUserDetail(id as string),
    enabled: Boolean(id),
    refetchOnWindowFocus: false,
  });
}

export function useAdminUserMutations() {
  const queryClient = useQueryClient();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ADMIN_USERS_KEY });

  const createUser = useMutation({
    mutationFn: (payload: CreateUserRequest) => adminApi.createUser(payload),
    onSuccess: invalidate,
  });

  const updateRole = useMutation({
    mutationFn: ({ id, role }: { id: string; role: UserRole }) => adminApi.updateRole(id, role),
    onSuccess: invalidate,
  });

  const updateStatus = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      adminApi.updateStatus(id, active),
    onSuccess: invalidate,
  });

  const resetPassword = useMutation({
    mutationFn: ({ id, newPassword }: { id: string; newPassword: string }) =>
      adminApi.resetPassword(id, newPassword),
  });

  const revokeSessions = useMutation({
    mutationFn: (id: string) => adminApi.revokeSessions(id),
    onSuccess: invalidate,
  });

  const deleteUser = useMutation({
    mutationFn: (id: string) => adminApi.deleteUser(id),
    onSuccess: invalidate,
  });

  return { createUser, updateRole, updateStatus, resetPassword, revokeSessions, deleteUser };
}
