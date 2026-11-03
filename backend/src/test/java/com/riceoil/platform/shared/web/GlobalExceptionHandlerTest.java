package com.riceoil.platform.shared.web;

import com.riceoil.platform.customers.application.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void illegalArgumentDoesNotEchoPrivateDetails() {
        var response = handler.handleIllegalArgument(
                new IllegalArgumentException("Customer not found: person@example.com"), request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Please check your information and try again.", response.getBody().message());
        assertFalse(response.getBody().message().contains("person@example.com"));
    }

    @Test
    void invalidCredentialsUseGenericUnauthorizedResponse() {
        var response = handler.handleInvalidCredentials(new InvalidCredentialsException(), request);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Email or password is incorrect.", response.getBody().message());
    }

    @Test
    void accessDeniedDoesNotBecomeInternalServerError() {
        var response = handler.handleAccessDenied(new AccessDeniedException("guest cart token is invalid"), request);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("You don't have permission to access this resource.", response.getBody().message());
    }
}