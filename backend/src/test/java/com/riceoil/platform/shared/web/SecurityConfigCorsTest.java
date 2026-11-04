package com.riceoil.platform.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityConfigCorsTest {

    @Test
    void configuresCommaSeparatedAllowedOrigins() {
        var source = new SecurityConfig().corsConfigurationSource(
                "https://store.example, https://admin.example");
        var request = new MockHttpServletRequest("GET", "/api/catalog");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertEquals(List.of("https://store.example", "https://admin.example"),
                configuration.getAllowedOriginPatterns());
    }
}
