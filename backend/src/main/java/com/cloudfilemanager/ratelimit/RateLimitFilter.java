package com.cloudfilemanager.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory fixed-window rate limiter, keyed by authenticated user (falling back to
 * client IP for anonymous requests, e.g. public share-link redemption). Deliberately not
 * backed by Redis/etc: this app runs as a single instance, so per-JVM state is sufficient
 * and keeps the feature dependency-free.
 *
 * This only ever throttles calls to OUR OWN API (JSON responses, short-lived) - actual file
 * bytes flow directly between the browser and S3/MinIO via presigned URLs (see Phase 3),
 * so rate limiting here never becomes a bottleneck for legitimate bulk file transfer.
 *
 * Always registered as a bean (SecurityFilterChainConfig wires it into the chain
 * unconditionally) - "enabled" is an internal pass-through switch rather than a
 * @ConditionalOnProperty, so disabling it can never leave a downstream bean with a
 * dependency on a filter that doesn't exist.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long WINDOW_MILLIS = 60_000;

    private final boolean enabled;
    private final int requestsPerWindow;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.requests-per-minute:120}") int requestsPerWindow) {
        this.enabled = enabled;
        this.requestsPerWindow = requestsPerWindow;
    }

    private static final class Window {
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger count = new AtomicInteger(0);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        Window window = windows.computeIfAbsent(key, k -> new Window());

        long now = System.currentTimeMillis();
        long start = window.windowStart.get();
        if (now - start >= WINDOW_MILLIS) {
            // New window: reset if we win the race to roll it over.
            if (window.windowStart.compareAndSet(start, now)) {
                window.count.set(0);
            }
        }

        int current = window.count.incrementAndGet();
        if (current > requestsPerWindow) {
            logger.warn("Rate limit exceeded for {}: {} requests in current window", key, current);
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"Too many requests. Please slow down and try again shortly.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddr();
        return "ip:" + ip;
    }
}
