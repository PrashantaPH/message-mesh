import { create } from 'zustand';
import type { LocalMessage, LocalMessageStatus, MessageDto } from '../types/dto';

interface ChatState {
  activeConversationId: string | null;
  messagesByConv: Record<string, LocalMessage[]>;
  drafts: Record<string, string>;
  replyByConv: Record<string, LocalMessage | null>;

  setActiveConversation: (id: string | null) => void;
  setMessages: (conversationId: string, messages: MessageDto[]) => void;
  addOptimistic: (message: LocalMessage) => void;
  /** Reconcile an optimistic message (by clientTempId) with the server echo. */
  upsertFromServer: (message: MessageDto) => void;
  updateStatus: (conversationId: string, messageId: string, status: LocalMessageStatus) => void;
  markFailed: (conversationId: string, clientTempId: string) => void;
  setDraft: (conversationId: string, draft: string) => void;
  setReplyingTo: (conversationId: string, message: LocalMessage | null) => void;
}

const sortBySeq = (messages: LocalMessage[]) =>
  [...messages].sort((a, b) => {
    // Optimistic (SENDING) messages have seq 0 — keep them last by createdAt.
    if (a.seq !== b.seq) return a.seq - b.seq;
    return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
  });

export const useChatStore = create<ChatState>((set) => ({
  activeConversationId: null,
  messagesByConv: {},
  drafts: {},
  replyByConv: {},

  setActiveConversation: (id) => set({ activeConversationId: id }),

  setMessages: (conversationId, messages) =>
    set((state) => ({
      messagesByConv: {
        ...state.messagesByConv,
        [conversationId]: sortBySeq(messages.map((m) => ({ ...m }))),
      },
    })),

  addOptimistic: (message) =>
    set((state) => {
      const list = state.messagesByConv[message.conversationId] ?? [];
      return {
        messagesByConv: {
          ...state.messagesByConv,
          [message.conversationId]: [...list, message],
        },
      };
    }),

  upsertFromServer: (message) =>
    set((state) => {
      const list = state.messagesByConv[message.conversationId] ?? [];
      let matched = false;
      const next = list.map((m) => {
        const sameId = m.id === message.id;
        const sameTemp =
          message.clientTempId && m.clientTempId && m.clientTempId === message.clientTempId;
        if (sameId || sameTemp) {
          matched = true;
          return { ...message, status: message.status };
        }
        return m;
      });
      if (!matched) next.push({ ...message });
      return {
        messagesByConv: {
          ...state.messagesByConv,
          [message.conversationId]: sortBySeq(next),
        },
      };
    }),

  updateStatus: (conversationId, messageId, status) =>
    set((state) => {
      const list = state.messagesByConv[conversationId] ?? [];
      return {
        messagesByConv: {
          ...state.messagesByConv,
          [conversationId]: list.map((m) => (m.id === messageId ? { ...m, status } : m)),
        },
      };
    }),

  markFailed: (conversationId, clientTempId) =>
    set((state) => {
      const list = state.messagesByConv[conversationId] ?? [];
      return {
        messagesByConv: {
          ...state.messagesByConv,
          [conversationId]: list.map((m) =>
            m.clientTempId === clientTempId ? { ...m, status: 'FAILED' } : m,
          ),
        },
      };
    }),

  setDraft: (conversationId, draft) =>
    set((state) => ({ drafts: { ...state.drafts, [conversationId]: draft } })),

  setReplyingTo: (conversationId, message) =>
    set((state) => ({ replyByConv: { ...state.replyByConv, [conversationId]: message } })),
}));
