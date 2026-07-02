package com.message.mesh.constant;

/**
 * Application-wide constants: STOMP destinations and shared keys.
 */
public final class AppConstants {

    private AppConstants() {
    }

    // STOMP application (client -> server) destinations
    public static final String APP_CHAT_SEND = "/app/chat.send";
    public static final String APP_CHAT_ACK = "/app/chat.ack";
    public static final String APP_CHAT_TYPING = "/app/chat.typing";

    // STOMP broker (server -> client) destination prefixes
    public static final String TOPIC_CONVERSATION_PREFIX = "/topic/conv.";
    public static final String TOPIC_PRESENCE = "/topic/presence";
    public static final String QUEUE_ACK = "/queue/ack";
    public static final String QUEUE_CONVERSATIONS = "/queue/conversations";

    public static String topicConversation(Object conversationId) {
        return TOPIC_CONVERSATION_PREFIX + conversationId;
    }

    public static String topicTyping(Object conversationId) {
        return TOPIC_CONVERSATION_PREFIX + conversationId + ".typing";
    }

    /** Conversation/membership meta events (rename, member add/remove, role change, delete). */
    public static String topicConversationMeta(Object conversationId) {
        return TOPIC_CONVERSATION_PREFIX + conversationId + ".meta";
    }

    // WebSocket / handshake attribute keys
    public static final String ATTR_USERNAME = "username";

    // Auth
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTH_HEADER = "Authorization";
}
