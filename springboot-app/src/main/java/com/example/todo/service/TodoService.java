package com.example.todo.service;

import com.example.todo.model.Todo;
import com.example.todo.model.TodoLog;
import com.example.todo.repository.TodoLogRepository;
import com.example.todo.repository.TodoRepository;
import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.exception.TodoTransactionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for handling all business logic related to Todos and Todo Logs.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In FastAPI, transaction control is often manual using SQLAlchemy's context managers
 * ({@code with session.begin():} or {@code try/except} blocks with {@code session.rollback()}).
 * In Spring Boot, transaction management is declarative. By annotating the service class or methods
 * with {@link Transactional}, Spring automatically opens a transaction, executes the method code,
 * and commits it. If a runtime exception is thrown, Spring automatically rolls back all database operations
 * executed within that transaction boundary.
 * </p>
 */
@Service
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    private final TodoLogRepository todoLogRepository;

    public TodoService(TodoRepository todoRepository, TodoLogRepository todoLogRepository) {
        this.todoRepository = todoRepository;
        this.todoLogRepository = todoLogRepository;
    }

    /**
     * Retrieve all Todo items.
     *
     * @return a list of all Todos
     */
    @Transactional(readOnly = true)
    public List<Todo> getAllTodos() {
        return todoRepository.findAll();
    }

    /**
     * Retrieve a specific Todo item by ID.
     *
     * @param id the ID of the Todo
     * @return the Todo if found, or empty
     */
    @Transactional(readOnly = true)
    public Optional<Todo> getTodoById(Long id) {
        return todoRepository.findById(id);
    }

    /**
     * Retrieve all log records.
     *
     * @return list of all TodoLog entries
     */
    @Transactional(readOnly = true)
    public List<TodoLog> getAllLogs() {
        return todoLogRepository.findAllByOrderByTimestampDesc();
    }

    /**
     * Create a new Todo item and write an audit log entry.
     * Triggers a rollback if the title is "force-rollback".
     *
     * @param todo the Todo to create
     * @return the created Todo
     */
    public Todo createTodo(Todo todo) {
        // Save the Todo first to generate an ID
        Todo savedTodo = todoRepository.save(todo);

        // Write a log entry
        TodoLog log = new TodoLog(
            savedTodo.getId(),
            "CREATE",
            "Created Todo: '" + savedTodo.getTitle() + "' (Completed: " + savedTodo.isCompleted() + ")"
        );
        todoLogRepository.save(log);

        // Check for force-rollback trigger
        if ("force-rollback".equalsIgnoreCase(savedTodo.getTitle())) {
            throw new TodoTransactionException("Forced transaction rollback triggered by title: 'force-rollback'");
        }

        return savedTodo;
    }

    /**
     * Update an existing Todo item and write an audit log entry.
     * Triggers a rollback if the new title is "force-rollback".
     *
     * @param id the ID of the Todo to update
     * @param updatedDetails the new details for the Todo
     * @return the updated Todo
     */
    public Todo updateTodo(Long id, Todo updatedDetails) {
        Todo existingTodo = todoRepository.findById(id)
            .orElseThrow(() -> new TodoNotFoundException("Todo with ID " + id + " not found"));

        String oldTitle = existingTodo.getTitle();
        boolean oldCompleted = existingTodo.isCompleted();

        // Update fields
        existingTodo.setTitle(updatedDetails.getTitle());
        existingTodo.setDescription(updatedDetails.getDescription());
        existingTodo.setCompleted(updatedDetails.isCompleted());

        Todo savedTodo = todoRepository.save(existingTodo);

        // Write log entry
        TodoLog log = new TodoLog(
            savedTodo.getId(),
            "UPDATE",
            String.format("Updated Todo ID %d. Title: '%s' -> '%s', Completed: %s -> %s",
                id, oldTitle, savedTodo.getTitle(), oldCompleted, savedTodo.isCompleted())
        );
        todoLogRepository.save(log);

        // Check for force-rollback trigger
        if ("force-rollback".equalsIgnoreCase(savedTodo.getTitle())) {
            throw new TodoTransactionException("Forced transaction rollback triggered by title: 'force-rollback'");
        }

        return savedTodo;
    }

    /**
     * Delete a Todo item and write an audit log entry.
     *
     * @param id the ID of the Todo to delete
     */
    public void deleteTodo(Long id) {
        Todo existingTodo = todoRepository.findById(id)
            .orElseThrow(() -> new TodoNotFoundException("Todo with ID " + id + " not found"));

        // Write log entry
        TodoLog log = new TodoLog(
            id,
            "DELETE",
            "Deleted Todo: '" + existingTodo.getTitle() + "'"
        );
        todoLogRepository.save(log);

        // Delete the Todo
        todoRepository.delete(existingTodo);
    }

    /**
     * Clears all logs in the database.
     */
    public void clearLogs() {
        todoLogRepository.deleteAll();
    }
}
