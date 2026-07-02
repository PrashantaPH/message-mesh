package com.message.mesh.security;

import com.message.mesh.constant.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private AppUserDetails userDetails(boolean active, int tokenVersion) {
        return new AppUserDetails(UUID.randomUUID(), "alice", "hash", active, tokenVersion,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("populates the security context for a valid token")
    void validTokenAuthenticates() throws Exception {
        when(request.getHeader(AppConstants.AUTH_HEADER)).thenReturn("Bearer good-token");
        when(jwtUtil.isValid("good-token")).thenReturn(true);
        when(jwtUtil.getUsername("good-token")).thenReturn("alice");
        when(jwtUtil.getTokenVersion("good-token")).thenReturn(0);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails(true, 0));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("skips authentication when there is no Authorization header")
    void noHeaderSkipsAuthentication() throws Exception {
        when(request.getHeader(AppConstants.AUTH_HEADER)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("does not authenticate when the token is invalid")
    void invalidTokenSkipsAuthentication() throws Exception {
        when(request.getHeader(AppConstants.AUTH_HEADER)).thenReturn("Bearer bad");
        when(jwtUtil.isValid("bad")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("rejects a token for a disabled account")
    void disabledAccountRejected() throws Exception {
        when(request.getHeader(AppConstants.AUTH_HEADER)).thenReturn("Bearer good");
        when(jwtUtil.isValid("good")).thenReturn(true);
        when(jwtUtil.getUsername("good")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails(false, 0));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("rejects a token whose version no longer matches the account")
    void staleTokenVersionRejected() throws Exception {
        when(request.getHeader(AppConstants.AUTH_HEADER)).thenReturn("Bearer good");
        when(jwtUtil.isValid("good")).thenReturn(true);
        when(jwtUtil.getUsername("good")).thenReturn("alice");
        when(jwtUtil.getTokenVersion("good")).thenReturn(1);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails(true, 2));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
