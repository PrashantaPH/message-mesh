package com.message.mesh.security;

import com.message.mesh.constant.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Captures a JWT supplied as a {@code ?token=} query parameter (or Authorization
 * header) during the WebSocket handshake. Enforcement of authentication happens
 * on the STOMP CONNECT frame ({@link WebSocketAuthChannelInterceptor}); this
 * interceptor only records the username hint and never blocks the SockJS
 * transport negotiation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token != null && jwtUtil.isValid(token)) {
            attributes.put(AppConstants.ATTR_USERNAME, jwtUtil.getUsername(token));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String header = servletRequest.getServletRequest().getHeader(AppConstants.AUTH_HEADER);
            if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
                return header.substring(AppConstants.BEARER_PREFIX.length());
            }
            String query = servletRequest.getServletRequest().getParameter("token");
            if (query != null && !query.isBlank()) {
                return query;
            }
        }
        return null;
    }
}
