package com.example.todo.exception;

/**
 * Custom exception thrown when a requested Todo item is not found in the database.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * This is equivalent to {@code TodoNotFoundError} in the FastAPI implementation.
 * In Spring Boot, we map this exception to an HTTP 404 (Not Found) status code
 * via a global {@code @RestControllerAdvice}.
 * </p>
 */
public class TodoNotFoundException extends RuntimeException {
    public TodoNotFoundException(String message) {
        super(message);
    }
}
