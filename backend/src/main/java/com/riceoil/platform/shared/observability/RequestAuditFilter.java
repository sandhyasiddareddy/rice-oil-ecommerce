package com.riceoil.platform.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs after Spring Security so the authenticated customer is known. Adds customerId and sessionId
 * (the JWT id issued at login) to the MDC and writes one summary line per API request.
 * The query string is deliberately not logged because it can carry access tokens.
 */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER + 1)
public class RequestAuditFilter extends OncePerRequestFilter {

    public static final String CUSTOMER_ID_KEY = "customerId";
    public static final String SESSION_ID_KEY = "sessionId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Object customerId = jwtAuthentication.getToken().getClaims().get("customerId");
            if (customerId != null) {
                MDC.put(CUSTOMER_ID_KEY, String.valueOf(customerId));
            }
            String sessionId = jwtAuthentication.getToken().getId();
            if (sessionId != null) {
                MDC.put(SESSION_ID_KEY, sessionId);
            }
        }

        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            BusinessLog.info("HTTP_REQUEST",
                    "method", request.getMethod(),
                    "path", request.getRequestURI(),
                    "status", response.getStatus(),
                    "durationMs", (System.nanoTime() - startedAt) / 1_000_000);
            MDC.remove(CUSTOMER_ID_KEY);
            MDC.remove(SESSION_ID_KEY);
        }
    }
}
