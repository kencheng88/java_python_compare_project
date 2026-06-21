package com.example.todo.exception;

/**
 * Custom exception thrown when a database transaction fails or is rolled back.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * This is equivalent to {@code TodoTransactionError} in the FastAPI implementation.
 * In Java/Spring, extending {@link RuntimeException} ensures that throwing this exception
 * inside a {@code @Transactional} method will automatically trigger a database rollback.
 * </p>
 */
public class TodoTransactionException extends RuntimeException {
    public TodoTransactionException(String message) {
        super(message);
    }
}
