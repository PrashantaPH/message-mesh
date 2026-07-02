import { createContext, useContext } from 'react';
import type { ConnectionStatus } from './StompClient';
import type { SendMessageRequest, TypingEvent } from '../types/dto';

export interface SocketContextValue {
  status: ConnectionStatus;
  sendMessage: (payload: SendMessageRequest) => void;
  sendTyping: (payload: TypingEvent) => void;
  ackMessage: (messageId: string) => void;
}

export const SocketContext = createContext<SocketContextValue | null>(null);

export const useSocket = (): SocketContextValue => {
  const ctx = useContext(SocketContext);
  if (!ctx) {
    throw new Error('useSocket must be used within <SocketProvider>');
  }
  return ctx;
};
