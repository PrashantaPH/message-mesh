import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export type ConnectionStatus = 'idle' | 'connecting' | 'connected' | 'reconnecting' | 'error';

/**
 * Thin singleton-style wrapper around a STOMP-over-SockJS connection that
 * matches the Spring backend contract. Buffers subscriptions until connected
 * and re-applies them automatically on reconnect.
 */
export class StompClient {
  private client: Client;
  private subscriptions = new Map<string, { cb: (msg: IMessage) => void; sub?: StompSubscription }>();
  private statusListeners = new Set<(status: ConnectionStatus) => void>();
  private _status: ConnectionStatus = 'idle';

  constructor(token: string) {
    this.client = new Client({
      webSocketFactory: () => new SockJS('https://message-mesh-dev.onrender.com/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        this.setStatus('connected');
        // (Re)apply all known subscriptions.
        this.subscriptions.forEach((entry, destination) => {
          entry.sub = this.client.subscribe(destination, entry.cb);
        });
      },
      onWebSocketClose: () => {
        if (this._status === 'connected') this.setStatus('reconnecting');
      },
      onStompError: () => this.setStatus('error'),
      onWebSocketError: () => this.setStatus('error'),
    });
  }

  get status(): ConnectionStatus {
    return this._status;
  }

  onStatus(listener: (status: ConnectionStatus) => void): () => void {
    this.statusListeners.add(listener);
    listener(this._status);
    return () => this.statusListeners.delete(listener);
  }

  activate(): void {
    this.setStatus('connecting');
    this.client.activate();
  }

  subscribe(destination: string, cb: (msg: IMessage) => void): () => void {
    const entry: { cb: (msg: IMessage) => void; sub?: StompSubscription } = { cb };
    if (this.client.connected) {
      entry.sub = this.client.subscribe(destination, cb);
    }
    this.subscriptions.set(destination, entry);
    return () => {
      const current = this.subscriptions.get(destination);
      current?.sub?.unsubscribe();
      this.subscriptions.delete(destination);
    };
  }

  publish(destination: string, body: unknown): void {
    this.client.publish({ destination, body: JSON.stringify(body) });
  }

  deactivate(): void {
    this.subscriptions.clear();
    void this.client.deactivate();
    this.setStatus('idle');
  }

  private setStatus(status: ConnectionStatus): void {
    this._status = status;
    this.statusListeners.forEach((l) => l(status));
  }
}
