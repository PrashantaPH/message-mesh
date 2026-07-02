import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../api/admin.api';

const ADMIN_STATS_KEY = ['admin', 'stats'] as const;

export function useAdminStats() {
  return useQuery({
    queryKey: ADMIN_STATS_KEY,
    queryFn: () => adminApi.getStats(),
    refetchOnWindowFocus: false,
  });
}
