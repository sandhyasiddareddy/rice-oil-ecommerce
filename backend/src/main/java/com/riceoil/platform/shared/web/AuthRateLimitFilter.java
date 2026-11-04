package com.riceoil.platform.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.riceoil.platform.shared.observability.BusinessLog;
import com.riceoil.platform.shared.observability.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window, in-memory limiter for authentication endpoints, keyed by client IP and path.
 * State is per instance; use a shared store if the backend is scaled beyond one instance.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "/api/auth/";

    private record Window(long startedAtMs, int count) {
    }

    private final boolean enabled;
    private final int maxRequests;
    private final long windowMs;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private volatile long lastSweepMs = System.currentTimeMillis();

    public AuthRateLimitFilter(@Value("${app.security.rate-limit.enabled:true}") boolean enabled,
                               @Value("${app.security.rate-limit.max-requests:10}") int maxRequests,
                               @Value("${app.security.rate-limit.window-seconds:60}") long windowSeconds) {
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled
                || !"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith(AUTH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        sweep(now);
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        Window updated = windows.compute(key, (k, current) ->
                current == null || now - current.startedAtMs() >= windowMs
                        ? new Window(now, 1)
                        : new Window(current.startedAtMs(), current.count() + 1));

        if (updated.count() > maxRequests) {
            long retryAfter = Math.max(1, (updated.startedAtMs() + windowMs - now + 999) / 1000);
            BusinessLog.warn("AUTH_RATE_LIMITED", "path", request.getRequestURI(), "ip", request.getRemoteAddr());
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
                    OffsetDateTime.now(),
                    429,
                    "RATE_LIMITED",
                    "Too many attempts. Please try again later.",
                    MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void sweep(long now) {
        if (now - lastSweepMs < windowMs) {
            return;
        }
        lastSweepMs = now;
        Iterator<Window> it = windows.values().iterator();
        while (it.hasNext()) {
            if (now - it.next().startedAtMs() >= windowMs) {
                it.remove();
            }
        }
    }
}
