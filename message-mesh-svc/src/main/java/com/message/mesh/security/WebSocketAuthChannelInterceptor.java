package com.message.mesh.security;

import com.message.mesh.constant.AppConstants;
import com.message.mesh.logging.MdcKeys;
import com.message.mesh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Authenticates STOMP CONNECT frames using the {@code Authorization} native
 * header and binds the resulting {@link java.security.Principal} to the session,
 * making {@code principal.getName()} available to {@code @MessageMapping} methods.
 * Also enriches the SLF4J MDC for every inbound frame.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);
            if (token == null || !jwtUtil.isValid(token)) {
                throw new IllegalArgumentException("Unauthorized STOMP CONNECT: missing or invalid token");
            }
            String username = jwtUtil.getUsername(token);
            boolean accepted = userRepository.findByUsername(username)
                    .filter(com.message.mesh.domain.User::isActive)
                    .filter(u -> u.getTokenVersion() == jwtUtil.getTokenVersion(token))
                    .isPresent();
            if (!accepted) {
                throw new IllegalArgumentException(
                        "Unauthorized STOMP CONNECT: account deactivated, missing, or token revoked");
            }
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
            accessor.setUser(auth);
            log.debug("STOMP CONNECT authenticated for user '{}'", username);
        }

        if (accessor.getUser() != null) {
            MDC.put(MdcKeys.USERNAME, accessor.getUser().getName());
        }
        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel,
                                    boolean sent, Exception ex) {
        MDC.remove(MdcKeys.USERNAME);
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(AppConstants.AUTH_HEADER);
        if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
            return header.substring(AppConstants.BEARER_PREFIX.length());
        }
        // Fallback: token captured at handshake time.
        Object attr = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get("token")
                : null;
        return attr instanceof String s ? s : null;
    }
}
