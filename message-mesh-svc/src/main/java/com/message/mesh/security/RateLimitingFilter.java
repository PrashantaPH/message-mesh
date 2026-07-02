package com.message.mesh.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Lightweight in-memory (per-instance) token-bucket rate limiter for abuse-prone
 * endpoints. Authentication endpoints are throttled aggressively to blunt
 * credential stuffing / brute force; admin endpoints get a higher ceiling.
 *
 * <p>Keying is per client IP + endpoint group. Buckets are held in a Caffeine
 * cache that evicts idle entries. For multi-instance deployments this should be
 * fronted by a shared store (e.g. Redis) or an edge/API-gateway limiter.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String AUTH_PATTERN = "/api/auth/**";
    private static final String ADMIN_PATTERN = "/api/admin/**";

    private final boolean enabled;
    private final long authCapacity;
    private final double authRefillPerSecond;
    private final long adminCapacity;
    private final double adminRefillPerSecond;

    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(50_000)
            .build();

    public RateLimitingFilter(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.auth.capacity:10}") long authCapacity,
            @Value("${app.rate-limit.auth.refill-per-minute:10}") long authRefillPerMinute,
            @Value("${app.rate-limit.admin.capacity:120}") long adminCapacity,
            @Value("${app.rate-limit.admin.refill-per-minute:120}") long adminRefillPerMinute) {
        this.enabled = enabled;
        this.authCapacity = authCapacity;
        this.authRefillPerSecond = authRefillPerMinute / 60.0;
        this.adminCapacity = adminCapacity;
        this.adminRefillPerSecond = adminRefillPerMinute / 60.0;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String uri = request.getRequestURI();
        return !PATH_MATCHER.match(AUTH_PATTERN, uri) && !PATH_MATCHER.match(ADMIN_PATTERN, uri);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean isAuth = PATH_MATCHER.match(AUTH_PATTERN, uri);
        long capacity = isAuth ? authCapacity : adminCapacity;
        double refillPerSecond = isAuth ? authRefillPerSecond : adminRefillPerSecond;
        String group = isAuth ? "auth" : "admin";

        String key = clientIp(request) + ":" + group;
        TokenBucket bucket = buckets.get(key, k -> new TokenBucket(capacity, refillPerSecond));

        if (bucket != null && bucket.tryConsume()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded for {} on group '{}'", clientIp(request), group);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        response.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Rate limit exceeded. Please slow down and try again shortly.\"}");
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Minimal thread-safe token bucket with continuous refill.
     */
    private static final class TokenBucket {

        private final long capacity;
        private final double refillPerNano;
        private double tokens;
        private long lastRefillNano;

        TokenBucket(long capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerNano = refillPerSecond / 1_000_000_000.0;
            this.tokens = capacity;
            this.lastRefillNano = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            long now = System.nanoTime();
            double refill = (now - lastRefillNano) * refillPerNano;
            if (refill > 0) {
                tokens = Math.min(capacity, tokens + refill);
                lastRefillNano = now;
            }
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
