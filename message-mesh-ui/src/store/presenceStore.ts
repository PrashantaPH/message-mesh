import { create } from 'zustand';

interface PresenceState {
  /** username -> online */
  onlineUsers: Record<string, boolean>;
  /** conversationId -> (username -> last typing timestamp ms) */
  typing: Record<string, Record<string, number>>;

  setOnline: (username: string, online: boolean) => void;
  setOnlineBulk: (usernames: string[]) => void;
  setTyping: (conversationId: string, username: string) => void;
  clearTyping: (conversationId: string, username: string) => void;
}

export const usePresenceStore = create<PresenceState>((set) => ({
  onlineUsers: {},
  typing: {},

  setOnline: (username, online) =>
    set((state) => ({ onlineUsers: { ...state.onlineUsers, [username]: online } })),

  setOnlineBulk: (usernames) =>
    set(() => ({
      onlineUsers: usernames.reduce<Record<string, boolean>>((acc, u) => {
        acc[u] = true;
        return acc;
      }, {}),
    })),

  setTyping: (conversationId, username) =>
    set((state) => ({
      typing: {
        ...state.typing,
        [conversationId]: {
          ...(state.typing[conversationId] ?? {}),
          [username]: Date.now(),
        },
      },
    })),

  clearTyping: (conversationId, username) =>
    set((state) => {
      const conv = { ...(state.typing[conversationId] ?? {}) };
      delete conv[username];
      return { typing: { ...state.typing, [conversationId]: conv } };
    }),
}));
