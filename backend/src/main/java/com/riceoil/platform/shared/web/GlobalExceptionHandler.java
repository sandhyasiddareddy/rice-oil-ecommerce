package com.riceoil.platform.shared.web;

import com.riceoil.platform.shared.observability.BusinessLog;
import com.riceoil.platform.shared.observability.CorrelationIdFilter;
import com.riceoil.platform.customers.application.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                           HttpServletRequest request) {
        return build(ex, HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource could not be found.", request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(DuplicateResourceException ex,
                                                           HttpServletRequest request) {
        return build(ex, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "We could not complete that request.", request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                     HttpServletRequest request) {
                                                                    return build(ex, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Email or password is incorrect.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                                HttpServletRequest request) {
        return build(ex, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You don't have permission to access this resource.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                  HttpServletRequest request) {
        return build(ex, HttpStatus.BAD_REQUEST, "INVALID_INPUT",
            "Please check your information and try again.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(MethodArgumentNotValidException ex,
                                                                  HttpServletRequest request) {
        return build(ex, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
            "Some information is missing or invalid. Please check and try again.", request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        return build(ex, HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION",
            "Please check your information and try again.", request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(MissingRequestHeaderException ex,
                                                                HttpServletRequest request) {
        return build(ex, HttpStatus.BAD_REQUEST, "MISSING_HEADER",
            "A required request detail is missing.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                      HttpServletRequest request) {
        return build(ex, HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "This request method is not supported for the requested resource.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        LOGGER.error("Unhandled API error correlationId={} path={}",
            MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY), request.getRequestURI(), ex);
        return build(ex, HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please retry or contact support.",
                request);
    }

    private ResponseEntity<ApiErrorResponse> build(Throwable ex,
                                                   HttpStatus status,
                                                   String code,
                                                   String message,
                                                   HttpServletRequest request) {
        if (status != HttpStatus.INTERNAL_SERVER_ERROR) {
            BusinessLog.warn("REQUEST_REJECTED",
                    "status", status.value(),
                    "code", code,
                    "method", request.getMethod(),
                    "path", request.getRequestURI(),
                    "exception", ex.getClass().getSimpleName(),
                    "reason", ex.getMessage());
        }
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                code,
                message,
                MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)
        );
        return ResponseEntity.status(status).body(body);
    }

}
