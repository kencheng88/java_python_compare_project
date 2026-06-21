package com.example.todo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global REST exception handler that intercepts domain-specific exceptions.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * This serves the same purpose as the {@code register_exception_handlers} function
 * in the FastAPI application. Rather than catching exceptions locally in each
 * controller method, Spring's {@link RestControllerAdvice} intercepts them globally
 * and transforms them into structured HTTP JSON responses.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Intercepts TodoTransactionException and maps it to HTTP 400 Bad Request.
     * Includes both message and detail fields for test/client compatibility.
     */
    @ExceptionHandler(TodoTransactionException.class)
    public ResponseEntity<Map<String, Object>> handleTodoTransactionException(TodoTransactionException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("detail", ex.getMessage());
        body.put("message", ex.getMessage());
        body.put("type", "TodoTransactionException");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Intercepts TodoNotFoundException and maps it to HTTP 404 Not Found.
     */
    @ExceptionHandler(TodoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTodoNotFoundException(TodoNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("detail", ex.getMessage());
        body.put("message", ex.getMessage());
        body.put("type", "TodoNotFoundException");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
