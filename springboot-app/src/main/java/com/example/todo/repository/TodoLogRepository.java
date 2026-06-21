package com.example.todo.repository;

import com.example.todo.model.TodoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing {@link TodoLog} entities.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * Similar to {@link TodoRepository}, this interface provides automated CRUD operations.
 * We include a method to retrieve logs ordered by their timestamp in descending order,
 * which will allow us to easily display the newest logs first in the UI.
 * </p>
 */
@Repository
public interface TodoLogRepository extends JpaRepository<TodoLog, Long> {
    
    /**
     * Retrieve all logs ordered by timestamp descending.
     *
     * @return list of todo logs sorted by timestamp descending
     */
    List<TodoLog> findAllByOrderByTimestampDesc();
}
