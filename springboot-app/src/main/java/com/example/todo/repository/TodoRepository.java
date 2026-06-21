package com.example.todo.repository;

import com.example.todo.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Todo} entities.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In Python/FastAPI using SQLModel or SQLAlchemy, querying is typically done programmatically using
 * `select(Todo).where(...)` or session-level queries. There is no built-in direct equivalent of Spring Data's
 * repository pattern unless custom repository classes are written.
 * In Spring Boot, Spring Data JPA automatically provides fully implemented CRUD operations and database interaction
 * logic simply by extending {@link JpaRepository}. You can also write custom query methods (like {@code findByCompleted})
 * using simple method naming conventions, and Spring will generate the underlying SQL at runtime.
 * </p>
 */
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
