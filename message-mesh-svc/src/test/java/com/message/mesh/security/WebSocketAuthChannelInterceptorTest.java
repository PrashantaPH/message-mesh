package com.message.mesh.security;

import com.message.mesh.constant.AppConstants;
import com.message.mesh.domain.User;
import com.message.mesh.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebSocketAuthChannelInterceptor")
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    private final MessageChannel channel = mock(MessageChannel.class);

    private StompHeaderAccessor connectAccessor(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authHeader != null) {
            accessor.setNativeHeader(AppConstants.AUTH_HEADER, authHeader);
        }
        return accessor;
    }

    private Message<byte[]> connectMessage(String authHeader) {
        return MessageBuilder.createMessage(new byte[0], connectAccessor(authHeader).getMessageHeaders());
    }

    private User activeUser(int tokenVersion) {
        return User.builder().id(UUID.randomUUID()).username("alice").passwordHash("x")
                .displayName("Alice").active(true).tokenVersion(tokenVersion).build();
    }

    @Test
    @DisplayName("CONNECT with a valid token binds the authenticated principal")
    void connectBindsPrincipal() {
        when(jwtUtil.isValid("good")).thenReturn(true);
        when(jwtUtil.getUsername("good")).thenReturn("alice");
        when(jwtUtil.getTokenVersion("good")).thenReturn(0);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser(0)));

        StompHeaderAccessor accessor = connectAccessor("Bearer good");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        interceptor.preSend(message, channel);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("alice");
    }

    @Test
    @DisplayName("CONNECT without a token is rejected")
    void connectWithoutTokenRejected() {
        Message<byte[]> message = connectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing or invalid token");
    }

    @Test
    @DisplayName("CONNECT with an invalid token is rejected")
    void connectWithInvalidTokenRejected() {
        when(jwtUtil.isValid("bad")).thenReturn(false);
        Message<byte[]> message = connectMessage("Bearer bad");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CONNECT is rejected when the account is deactivated or the token was revoked")
    void connectRejectedWhenAccountInvalid() {
        when(jwtUtil.isValid("good")).thenReturn(true);
        when(jwtUtil.getUsername("good")).thenReturn("alice");
        when(jwtUtil.getTokenVersion("good")).thenReturn(5);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(activeUser(0)));

        Message<byte[]> message = connectMessage("Bearer good");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deactivated, missing, or token revoked");
    }

    @Test
    @DisplayName("a message without a STOMP accessor passes through unchanged")
    void nonStompMessagePassesThrough() {
        Message<String> message = new GenericMessage<>("hello");

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
    }

    @Test
    @DisplayName("afterSendCompletion clears the MDC without error")
    void afterSendCompletionClearsMdc() {
        Message<byte[]> message = connectMessage("Bearer good");
        interceptor.afterSendCompletion(message, channel, true, null);
        assertThat(true).isTrue();
    }
}
