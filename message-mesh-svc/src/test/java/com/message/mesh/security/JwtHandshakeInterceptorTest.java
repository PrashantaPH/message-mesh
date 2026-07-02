package com.message.mesh.security;

import com.message.mesh.constant.AppConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtHandshakeInterceptor")
class JwtHandshakeInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtHandshakeInterceptor interceptor;

    private final ServerHttpResponse response = mock(ServerHttpResponse.class);
    private final WebSocketHandler handler = mock(WebSocketHandler.class);

    @Test
    @DisplayName("captures the username from a valid Authorization header")
    void capturesUsernameFromHeader() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader(AppConstants.AUTH_HEADER)).thenReturn("Bearer good");
        when(jwtUtil.isValid("good")).thenReturn(true);
        when(jwtUtil.getUsername("good")).thenReturn("alice");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        Map<String, Object> attributes = new HashMap<>();

        boolean proceed = interceptor.beforeHandshake(request, response, handler, attributes);

        assertThat(proceed).isTrue();
        assertThat(attributes).containsEntry(AppConstants.ATTR_USERNAME, "alice");
    }

    @Test
    @DisplayName("captures the username from a valid ?token query parameter")
    void capturesUsernameFromQueryParam() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader(AppConstants.AUTH_HEADER)).thenReturn(null);
        when(servletRequest.getParameter("token")).thenReturn("good");
        when(jwtUtil.isValid("good")).thenReturn(true);
        when(jwtUtil.getUsername("good")).thenReturn("bob");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        Map<String, Object> attributes = new HashMap<>();

        interceptor.beforeHandshake(request, response, handler, attributes);

        assertThat(attributes).containsEntry(AppConstants.ATTR_USERNAME, "bob");
    }

    @Test
    @DisplayName("does not record a username when no token is supplied")
    void noTokenLeavesAttributesEmpty() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader(AppConstants.AUTH_HEADER)).thenReturn(null);
        when(servletRequest.getParameter("token")).thenReturn(null);
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        Map<String, Object> attributes = new HashMap<>();

        boolean proceed = interceptor.beforeHandshake(request, response, handler, attributes);

        assertThat(proceed).isTrue();
        assertThat(attributes).isEmpty();
    }

    @Test
    @DisplayName("ignores an invalid token")
    void invalidTokenIgnored() {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getHeader(AppConstants.AUTH_HEADER)).thenReturn("Bearer bad");
        when(jwtUtil.isValid("bad")).thenReturn(false);
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        Map<String, Object> attributes = new HashMap<>();

        interceptor.beforeHandshake(request, response, handler, attributes);

        assertThat(attributes).isEmpty();
    }

    @Test
    @DisplayName("proceeds for a non-servlet request without extracting a token")
    void nonServletRequestProceeds() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        Map<String, Object> attributes = new HashMap<>();

        boolean proceed = interceptor.beforeHandshake(request, response, handler, attributes);

        assertThat(proceed).isTrue();
        assertThat(attributes).isEmpty();
    }

    @Test
    @DisplayName("afterHandshake is a safe no-op")
    void afterHandshakeIsNoOp() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);

        interceptor.afterHandshake(request, response, handler, null);
        // no exception expected
        assertThat(true).isTrue();
    }
}
