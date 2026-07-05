package org.opentron.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
        body.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(NullPointerException ex, WebRequest request) {
        logger.error("Null pointer exception", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "A required value was null");
        body.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle resource not found scenarios
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle invalid state scenarios
     */
    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStateException(InvalidStateException ex, WebRequest request) {
        logger.warn("Invalid state: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Invalid State");
        body.put("message", ex.getMessage());
        body.put("timestamp", System.currentTimeMillis());
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
