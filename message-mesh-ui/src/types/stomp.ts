// types/stomp.ts — destinations match backend STOMP map (AppConstants)
export const STOMP = {
  send: '/app/chat.send',
  ack: '/app/chat.ack',
  typing: '/app/chat.typing',
  topicConversation: (id: string) => `/topic/conv.${id}`,
  topicTyping: (id: string) => `/topic/conv.${id}.typing`,
  topicConversationMeta: (id: string) => `/topic/conv.${id}.meta`,
  topicPresence: '/topic/presence',
  userAck: '/user/queue/ack',
  userConversations: '/user/queue/conversations',
} as const;
