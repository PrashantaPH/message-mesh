package com.message.mesh.security;

import com.message.mesh.constant.AppConstants;
import com.message.mesh.logging.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the {@code Authorization: Bearer <jwt>} header on each request and
 * populates the {@link SecurityContextHolder} when valid.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(AppConstants.AUTH_HEADER);
        if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
            String token = header.substring(AppConstants.BEARER_PREFIX.length());
            if (jwtUtil.isValid(token)) {
                String username = jwtUtil.getUsername(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (isTokenAccepted(userDetails, token)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        MDC.put(MdcKeys.USERNAME, username);
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Accepts the token only when the account is enabled and the token's version
     * claim matches the account's current {@code tokenVersion} (forced-revocation
     * check).
     */
    private boolean isTokenAccepted(UserDetails userDetails, String token) {
        if (!userDetails.isEnabled()) {
            return false;
        }
        if (userDetails instanceof AppUserDetails app) {
            return app.getTokenVersion() == jwtUtil.getTokenVersion(token);
        }
        return true;
    }
}
