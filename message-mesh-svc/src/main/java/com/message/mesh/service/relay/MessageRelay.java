package com.message.mesh.service.relay;

import com.message.mesh.dto.MessageDto;

import java.util.UUID;

/**
 * Abstraction for fanning messages out to subscribers.
 *
 * <p>The default {@link InMemoryMessageRelay} uses Spring's in-memory simple
 * broker. Swapping to Redis/Kafka later only requires a new implementation —
 * no service-layer changes.
 */
public interface MessageRelay {

    /** Fan a message out to all subscribers of a conversation. */
    void relay(UUID conversationId, MessageDto payload);

    /** Send to a single user's private queue/destination. */
    void relayToUser(String username, String destination, Object payload);

    /** Broadcast to an arbitrary topic destination. */
    void broadcast(String destination, Object payload);
}
