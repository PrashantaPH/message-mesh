import { axiosClient } from './axiosClient';
import type { AuthResponse, ChangePasswordRequest, UserDto } from '../types/dto';

export const userApi = {
  me: () => axiosClient.get<UserDto>('/api/users/me').then((r) => r.data),

  list: () => axiosClient.get<UserDto[]>('/api/users').then((r) => r.data),

  online: () => axiosClient.get<string[]>('/api/users/online').then((r) => r.data),

  updateMe: (displayName: string) =>
    axiosClient.patch<UserDto>('/api/users/me', { displayName }).then((r) => r.data),

  changePassword: (payload: ChangePasswordRequest) =>
    axiosClient.post<AuthResponse>('/api/users/me/password', payload).then((r) => r.data),

  deleteMe: () => axiosClient.delete<void>('/api/users/me').then((r) => r.data),
};
