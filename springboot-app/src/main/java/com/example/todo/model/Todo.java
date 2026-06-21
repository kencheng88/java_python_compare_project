package com.example.todo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a Todo item.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In Python projects using SQLModel, SQLAlchemy, or Tortoise-ORM, models are defined using Python classes
 * that inherit from ORM bases (e.g., {@code SQLModel} or {@code Base}). Python ORMs use dynamic typing
 * and attributes declared as {@code Field()} or {@code Column()}.
 * In Java, JPA (Java Persistence API) uses explicit static typing and annotations like {@link Entity},
 * {@link Table}, {@link Id}, and {@link Column} to define mappings. Java entities also require standard
 * constructors and getter/setter methods. We leverage {@code PrePersist} and {@code PreUpdate} hooks
 * for handling timestamps, whereas in Python this is often configured with {@code default=datetime.utcnow}
 * or SQLAlchemy's {@code server_default}.
 * </p>
 */
@Entity
@Table(name = "todos")
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Todo() {
    }

    public Todo(String title, String description, boolean completed) {
        this.title = title;
        this.description = description;
        this.completed = completed;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
