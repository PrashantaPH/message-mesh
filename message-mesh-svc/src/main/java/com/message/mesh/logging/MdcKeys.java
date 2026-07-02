package com.message.mesh.logging;

/**
 * Keys placed into the SLF4J {@link org.slf4j.MDC} for structured, traceable logs.
 */
public final class MdcKeys {

    private MdcKeys() {
    }

    /** Correlation id for an inbound request / WS frame. */
    public static final String REQUEST_ID = "requestId";

    /** Authenticated username (when available). */
    public static final String USERNAME = "username";

    /** Conversation id in scope (set during message handling). */
    public static final String CONVERSATION_ID = "conversationId";
}
