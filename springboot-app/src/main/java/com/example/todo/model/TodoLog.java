package com.example.todo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing log records for changes made to Todo items.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In a FastAPI + SQLModel setup, secondary logging or audit tables are typically populated using event listeners,
 * background tasks, or direct session inserts within the same route function.
 * In Spring Boot, JPA model mappings for audit tables are straightforward entities. By managing these inside a
 * {@code @Transactional} service, any exception thrown during Todo creation/modification will automatically
 * roll back both the Todo insert/update and the log entry, maintaining database consistency across tables.
 * </p>
 */
@Entity
@Table(name = "todo_logs")
public class TodoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "todo_id")
    private Long todoId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    public TodoLog() {
    }

    public TodoLog(Long todoId, String action, String details) {
        this.todoId = todoId;
        this.action = action;
        this.details = details;
    }

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTodoId() {
        return todoId;
    }

    public void setTodoId(Long todoId) {
        this.todoId = todoId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
