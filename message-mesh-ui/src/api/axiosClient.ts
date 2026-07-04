import axios from 'axios';
import { getToken, useAuthStore } from '../store/authStore';

export const axiosClient = axios.create({
  baseURL: 'https://message-mesh-dev.onrender.com',
  headers: { 'Content-Type': 'application/json' },
});

axiosClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired/invalid — force re-auth.
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  },
);
