import { axiosClient } from './axiosClient';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/dto';

export const authApi = {
  login: (payload: LoginRequest) =>
    axiosClient.post<AuthResponse>('/api/auth/login', payload).then((r) => r.data),

  register: (payload: RegisterRequest) =>
    axiosClient.post<AuthResponse>('/api/auth/register', payload).then((r) => r.data),
};
