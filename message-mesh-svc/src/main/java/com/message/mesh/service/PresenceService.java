package com.message.mesh.service;

import com.message.mesh.constant.AppConstants;
import com.message.mesh.dto.PresenceDto;
import com.message.mesh.service.relay.MessageRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks online presence in-memory (per JVM). Multiple sessions per user are
 * supported; a user is considered offline only when their last session closes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final Map<String, Set<String>> onlineUsers = new ConcurrentHashMap<>();
    private final MessageRelay relay;

    public void markOnline(String username, String sessionId) {
        onlineUsers.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.debug("User '{}' online (session {})", username, sessionId);
        broadcastPresence(username, true);
    }

    public void markOffline(String username, String sessionId) {
        Set<String> sessions = onlineUsers.get(username);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                onlineUsers.remove(username);
                log.debug("User '{}' offline", username);
                broadcastPresence(username, false);
            }
        }
    }

    public boolean isOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public Set<String> onlineUsernames() {
        return Set.copyOf(onlineUsers.keySet());
    }

    private void broadcastPresence(String username, boolean online) {
        relay.broadcast(AppConstants.TOPIC_PRESENCE, new PresenceDto(username, online));
    }
}
