import { useEffect, useState } from 'react';
import { usePresenceStore } from '../store/presenceStore';

const TYPING_TTL_MS = 4000;

export function useIsOnline(username?: string): boolean {
  return usePresenceStore((s) => (username ? Boolean(s.onlineUsers[username]) : false));
}

/**
 * Returns the set of usernames currently typing in a conversation, expiring
 * entries older than {@link TYPING_TTL_MS}.
 */
export function useTypingUsers(conversationId: string | null): string[] {
  const typingMap = usePresenceStore((s) => (conversationId ? s.typing[conversationId] : undefined));
  const [, tick] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => tick((n) => n + 1), 1000);
    return () => clearInterval(interval);
  }, []);

  if (!typingMap) return [];
  const now = Date.now();
  return Object.entries(typingMap)
    .filter(([, ts]) => now - ts < TYPING_TTL_MS)
    .map(([username]) => username);
}
