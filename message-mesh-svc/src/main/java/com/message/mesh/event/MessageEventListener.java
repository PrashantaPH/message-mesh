package com.message.mesh.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageEventListener {

    /**
     * Non-blocking handler for post-persist side-effects. MDC context is
     * propagated by the async executor's task decorator.
     */
    @Async
    @EventListener
    public void onMessageCreated(MessageCreatedEvent event) {
        log.info("Side-effects for message {} in conversation {} (push, unread counters, analytics)",
                event.message().id(), event.message().conversationId());
        // Hook points: push notifications, unread counters, search indexing, etc.
    }
}
