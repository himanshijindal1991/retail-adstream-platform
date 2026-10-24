
package com.retail.insights_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Common JSON error response
    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String message,
            HttpServletRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }

    // 404 - Campaign not found
    @ExceptionHandler(CampaignNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(
            CampaignNotFoundException ex,
            HttpServletRequest request) {

        return error(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    // 400 - Invalid or missing request parameters
    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            MissingRequestHeaderException.class
    })
    public ResponseEntity<Map<String, Object>> badRequest(
            Exception ex,
            HttpServletRequest request) {

        return error(
                HttpStatus.BAD_REQUEST,
                "Invalid or missing request parameter",
                request
        );
    }

    // 403 - Tenant access denied
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied for request: {}",
                request.getRequestURI());

        return error(
                HttpStatus.FORBIDDEN,
                "You are not authorized to access this tenant",
                request
        );
    }

    // 500 - Unexpected server error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error while handling {}",
                request.getRequestURI(), ex);

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                request
        );
    }
}
