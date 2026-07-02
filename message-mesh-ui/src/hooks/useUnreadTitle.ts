import { useEffect } from 'react';
import { useConversations } from './useConversations';

const BASE_TITLE = 'MessageMesh — Real-Time Chat';

/**
 * Keeps the browser tab title in sync with the total unread message count,
 * e.g. "(3) MessageMesh — Real-Time Chat". Restores the base title on unmount.
 */
export function useUnreadTitle(): void {
  const { data: conversations } = useConversations();
  const totalUnread = (conversations ?? []).reduce(
    (sum, c) => sum + (c.unreadCount ?? 0),
    0,
  );

  useEffect(() => {
    document.title = totalUnread > 0 ? `(${totalUnread}) ${BASE_TITLE}` : BASE_TITLE;
    return () => {
      document.title = BASE_TITLE;
    };
  }, [totalUnread]);
}
