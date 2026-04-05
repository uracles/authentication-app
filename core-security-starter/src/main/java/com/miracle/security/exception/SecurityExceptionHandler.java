package com.miracle.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miracle.security.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

/**
 * Centralised security exception handling provided by the starter.
 * I made the design decision of implementing both Servlet-level handlers AND a @RestControllerAdvice in the
 * same class keeps the "error envelope" logic in one place. The Servlet handlers write JSON
 * directly to the response because Spring MVC's DispatcherServlet is not in the call stack
 * at that point (the exception originates in the security filter chain).
 */
@RestControllerAdvice
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);
    private final ObjectMapper objectMapper;

    public SecurityExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthenticated access attempt: {} {}", request.getMethod(), request.getRequestURI());
        writeError(response, request,
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Authentication is required to access this resource");
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.warn("Forbidden access attempt: {} {}", request.getMethod(), request.getRequestURI());
        writeError(response, request,
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "You do not have permission to access this resource");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied: {}", ex.getMessage());
        ApiErrorResponse body = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage(), request.getRequestURI());
        return org.springframework.http.ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    private void writeError(HttpServletResponse response,
                            HttpServletRequest request,
                            HttpStatus status,
                            String error,
                            String message) throws IOException {

        ApiErrorResponse body = ApiErrorResponse.of(status.value(), error, message, request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
