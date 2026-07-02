package com.message.mesh.event;

import com.message.mesh.service.PresenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketEventListener")
class WebSocketEventListenerTest {

    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private WebSocketEventListener listener;

    private Message<byte[]> stompMessage(StompCommand command, String sessionId, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (sessionId != null) {
            accessor.setSessionId(sessionId);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("onConnected marks the resolved user online")
    void onConnectedMarksOnline() {
        Principal user = () -> "alice";
        Message<byte[]> message = stompMessage(StompCommand.CONNECTED, "sess-1", user);
        SessionConnectedEvent event = new SessionConnectedEvent(this, message, user);

        listener.onConnected(event);

        verify(presenceService).markOnline("alice", "sess-1");
    }

    @Test
    @DisplayName("onConnected does nothing when there is no principal")
    void onConnectedIgnoresAnonymous() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECTED, "sess-1", null);
        SessionConnectedEvent event = new SessionConnectedEvent(this, message);

        listener.onConnected(event);

        verify(presenceService, never()).markOnline(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("onDisconnected marks the resolved user offline")
    void onDisconnectedMarksOffline() {
        Principal user = () -> "alice";
        Message<byte[]> message = stompMessage(StompCommand.DISCONNECT, "sess-2", user);
        SessionDisconnectEvent event =
                new SessionDisconnectEvent(this, message, "sess-2", null, user);

        listener.onDisconnected(event);

        verify(presenceService).markOffline("alice", "sess-2");
    }

    @Test
    @DisplayName("onDisconnected does nothing when there is no principal")
    void onDisconnectedIgnoresAnonymous() {
        Message<byte[]> message = stompMessage(StompCommand.DISCONNECT, "sess-2", null);
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, "sess-2", null);

        listener.onDisconnected(event);

        verify(presenceService, never()).markOffline(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
