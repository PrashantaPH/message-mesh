package com.message.mesh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.message.mesh.domain.ChatMessage;
import com.message.mesh.domain.Conversation;
import com.message.mesh.domain.Membership;
import com.message.mesh.domain.User;
import com.message.mesh.dto.AckDto;
import com.message.mesh.dto.AckRequest;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.SendMessageRequest;
import com.message.mesh.dto.TypingEvent;
import com.message.mesh.dto.TypingNotification;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.enums.MessageStatus;
import com.message.mesh.repository.ConversationRepository;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.MessageRepository;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration tests for {@link ChatController}'s STOMP endpoints.
 *
 * <p>Boots the full application on a random port and drives it through a real
 * SockJS/STOMP client over WebSocket, exercising JWT authentication on CONNECT,
 * message fan-out, acknowledgements and typing notifications.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatControllerTest {

    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MembershipRepository membershipRepository;
    @Autowired
    private MessageRepository messageRepository;

    private WebSocketStompClient stompClient;
    private Conversation conversation;
    private String aliceToken;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        membershipRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        User alice = userRepository.save(User.builder()
                .username("alice").passwordHash("x").displayName("Alice").build());
        User bob = userRepository.save(User.builder()
                .username("bob").passwordHash("x").displayName("Bob").build());

        conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT).build());
        membershipRepository.save(Membership.builder()
                .userId(alice.getId()).conversationId(conversation.getId()).build());
        membershipRepository.save(Membership.builder()
                .userId(bob.getId()).conversationId(conversation.getId()).build());

        aliceToken = jwtUtil.generateToken("alice");

        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        stompClient = new WebSocketStompClient(new SockJsClient(transports));
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("chat.send persists the message and broadcasts it to the conversation topic")
    void sendPersistsMessageAndBroadcastsToConversationTopic() throws Exception {
        StompSession session = connect(aliceToken);
        BlockingQueue<MessageDto> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/conv." + conversation.getId(),
                collectingHandler(MessageDto.class, received));

        session.send("/app/chat.send",
                new SendMessageRequest(conversation.getId(), "hello world", "tmp-1", null));

        MessageDto dto = received.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(dto).isNotNull();
        assertThat(dto.conversationId()).isEqualTo(conversation.getId());
        assertThat(dto.senderUsername()).isEqualTo("alice");
        assertThat(dto.body()).isEqualTo("hello world");
        assertThat(dto.clientTempId()).isEqualTo("tmp-1");
        assertThat(dto.seq()).isEqualTo(1L);
        assertThat(dto.status()).isEqualTo(MessageStatus.SENT);

        assertThat(messageRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("chat.ack flips the message status to DELIVERED and notifies the sender's user queue")
    void ackUpdatesStatusAndNotifiesSender() throws Exception {
        ChatMessage seeded = messageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .senderUsername("bob")
                .seq(1L)
                .body("hi alice")
                .status(MessageStatus.SENT)
                .createdAt(Instant.now())
                .build());

        StompSession session = connect(aliceToken);
        BlockingQueue<AckDto> received = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/ack", collectingHandler(AckDto.class, received));

        session.send("/app/chat.ack", new AckRequest(seeded.getId()));

        AckDto ack = received.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(ack).isNotNull();
        assertThat(ack.messageId()).isEqualTo(seeded.getId());
        assertThat(ack.status()).isEqualTo(MessageStatus.DELIVERED);

        assertThat(messageRepository.findById(seeded.getId()))
                .get()
                .extracting(ChatMessage::getStatus)
                .isEqualTo(MessageStatus.DELIVERED);
    }

    @Test
    @DisplayName("chat.typing broadcasts a typing notification carrying the sender's username")
    void typingBroadcastsNotification() throws Exception {
        StompSession session = connect(aliceToken);
        BlockingQueue<TypingNotification> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/conv." + conversation.getId() + ".typing",
                collectingHandler(TypingNotification.class, received));

        session.send("/app/chat.typing", new TypingEvent(conversation.getId()));

        TypingNotification note = received.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(note).isNotNull();
        assertThat(note.conversationId()).isEqualTo(conversation.getId());
        assertThat(note.username()).isEqualTo("alice");
    }

    @Test
    @DisplayName("STOMP CONNECT without a valid JWT is rejected")
    void connectWithoutTokenIsRejected() {
        assertThatThrownBy(() -> connect(null))
                .isInstanceOfAny(ExecutionException.class, TimeoutException.class);
    }

    private StompSession connect(String token) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        return stompClient.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        })
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static <T> StompFrameHandler collectingHandler(Class<T> type, BlockingQueue<T> sink) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return type;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                sink.add(type.cast(payload));
            }
        };
    }
}
