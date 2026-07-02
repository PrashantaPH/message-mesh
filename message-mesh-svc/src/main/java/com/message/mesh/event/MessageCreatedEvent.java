package com.message.mesh.event;

import com.message.mesh.dto.MessageDto;

/**
 * Published after a message is persisted so side-effects (notifications, unread
 * counters, analytics) can run asynchronously without blocking delivery.
 */
public record MessageCreatedEvent(MessageDto message) {
}
