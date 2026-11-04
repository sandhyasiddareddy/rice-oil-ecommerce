package com.riceoil.platform.shared.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AuthRateLimitFilterTest {

    private MockHttpServletResponse call(AuthRateLimitFilter filter, String method, String uri, String ip,
                                         FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void blocksAfterLimitPerIpAndPath() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(true, 3, 60);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 3; i++) {
            assertEquals(200, call(filter, "POST", "/api/auth/login", "1.1.1.1", chain).getStatus());
        }
        MockHttpServletResponse blocked = call(filter, "POST", "/api/auth/login", "1.1.1.1", chain);

        assertEquals(429, blocked.getStatus());
        assertTrue(blocked.getContentAsString().contains("RATE_LIMITED"));
        assertTrue(Integer.parseInt(blocked.getHeader("Retry-After")) >= 1);
        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        assertEquals(200, call(filter, "POST", "/api/auth/login", "2.2.2.2", chain).getStatus());
        assertEquals(200, call(filter, "POST", "/api/auth/register", "1.1.1.1", chain).getStatus());
    }

    @Test
    void ignoresNonAuthAndNonPostRequests() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(true, 1, 60);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            assertEquals(200, call(filter, "GET", "/api/auth/login", "1.1.1.1", chain).getStatus());
            assertEquals(200, call(filter, "POST", "/api/catalog/products", "1.1.1.1", chain).getStatus());
        }
    }

    @Test
    void canBeDisabled() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false, 1, 60);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            assertEquals(200, call(filter, "POST", "/api/auth/login", "1.1.1.1", chain).getStatus());
        }
    }
}
