package com.message.mesh.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitingFilter")
class RateLimitingFilterTest {

    private HttpServletRequest request(String uri, String ip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getRemoteAddr()).thenReturn(ip);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        return request;
    }

    @Test
    @DisplayName("shouldNotFilter is true when rate limiting is disabled")
    void disabledSkipsFiltering() {
        RateLimitingFilter filter = new RateLimitingFilter(false, 10, 10, 120, 120);

        assertThat(filter.shouldNotFilter(request("/api/auth/login", "1.1.1.1"))).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter is true for endpoints outside the throttled groups")
    void nonThrottledPathSkipped() {
        RateLimitingFilter filter = new RateLimitingFilter(true, 10, 10, 120, 120);

        assertThat(filter.shouldNotFilter(request("/api/conversations", "1.1.1.1"))).isTrue();
        assertThat(filter.shouldNotFilter(request("/api/users/me", "1.1.1.1"))).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter is false for auth and admin endpoints")
    void throttledPathsFiltered() {
        RateLimitingFilter filter = new RateLimitingFilter(true, 10, 10, 120, 120);

        assertThat(filter.shouldNotFilter(request("/api/auth/login", "1.1.1.1"))).isFalse();
        assertThat(filter.shouldNotFilter(request("/api/admin/users", "1.1.1.1"))).isFalse();
    }

    @Test
    @DisplayName("requests within the capacity are allowed through the chain")
    void allowsWithinCapacity() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, 2, 0, 120, 120);
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        filter.doFilterInternal(request("/api/auth/login", "9.9.9.9"), response, chain);
        filter.doFilterInternal(request("/api/auth/login", "9.9.9.9"), response, chain);

        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    @DisplayName("requests beyond the capacity receive HTTP 429 and are not forwarded")
    void blocksBeyondCapacity() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, 1, 0, 120, 120);
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        HttpServletRequest first = request("/api/auth/login", "8.8.8.8");
        HttpServletRequest second = request("/api/auth/login", "8.8.8.8");

        filter.doFilterInternal(first, response, chain);
        filter.doFilterInternal(second, response, chain);

        verify(chain, times(1)).doFilter(any(), any());
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setHeader("Retry-After", "60");
        assertThat(body.toString()).contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("the client IP is taken from X-Forwarded-For when present")
    void usesForwardedForHeader() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, 1, 0, 120, 120);
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        HttpServletRequest a = mock(HttpServletRequest.class);
        when(a.getRequestURI()).thenReturn("/api/auth/login");
        when(a.getHeader("X-Forwarded-For")).thenReturn("5.5.5.5, 6.6.6.6");
        HttpServletRequest b = mock(HttpServletRequest.class);
        when(b.getRequestURI()).thenReturn("/api/auth/login");
        when(b.getHeader("X-Forwarded-For")).thenReturn("5.5.5.5, 6.6.6.6");

        filter.doFilterInternal(a, response, chain);
        filter.doFilterInternal(b, response, chain);

        // Same forwarded client → second request throttled.
        verify(chain, times(1)).doFilter(any(), any());
    }
}
