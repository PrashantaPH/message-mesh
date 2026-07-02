import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { adminApi, type ListAuditParams } from '../api/admin.api';

const AUDIT_KEY = ['admin', 'audit'] as const;

export function useAuditLog(params: ListAuditParams = {}) {
  const { actor = '', action = '', from, to, page = 0, size = 20 } = params;
  return useQuery({
    queryKey: [...AUDIT_KEY, { actor, action, from: from ?? null, to: to ?? null, page, size }],
    queryFn: () =>
      adminApi.listAudit({
        actor: actor || undefined,
        action: action || undefined,
        from: from || undefined,
        to: to || undefined,
        page,
        size,
      }),
    refetchOnWindowFocus: false,
    placeholderData: keepPreviousData,
  });
}
