import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth.api';
import { useAuthStore } from '../store/authStore';
import type { LoginRequest, RegisterRequest } from '../types/dto';

export function useAuth() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const logout = useAuthStore((s) => s.logout);
  const user = useAuthStore((s) => s.user);
  const token = useAuthStore((s) => s.token);

  const loginMutation = useMutation({
    mutationFn: (payload: LoginRequest) => authApi.login(payload),
    onSuccess: (data) => {
      setAuth(data.token, data.user);
      navigate('/chats');
    },
  });

  const registerMutation = useMutation({
    mutationFn: (payload: RegisterRequest) => authApi.register(payload),
    onSuccess: (data) => {
      setAuth(data.token, data.user);
      navigate('/chats');
    },
  });

  return {
    user,
    token,
    isAuthenticated: Boolean(token),
    isAdmin: user?.role === 'ADMIN',
    login: loginMutation,
    register: registerMutation,
    logout: () => {
      logout();
      navigate('/login');
    },
  };
}
