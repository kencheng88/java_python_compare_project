package com.example.todo.controller;

import com.example.todo.model.Todo;
import com.example.todo.model.TodoLog;
import com.example.todo.service.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for the Todo and Logging API endpoints.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In FastAPI, path decorators such as {@code @app.get("/api/todos")} specify the HTTP method, status codes,
 * and responses directly on route functions. Security checks are passed as parameters using {@code Depends()}.
 * In Spring Boot, we use mapping annotations ({@link GetMapping}, {@link PostMapping}, etc.) at the method
 * level. Security is handled via Method Security annotations like {@link PreAuthorize}, which accept Spring
 * Expression Language (SpEL) expressions. Exception handling is centralized using {@link ExceptionHandler}
 * annotations, preventing boilerplate try-catch blocks within the endpoint logic.
 * </p>
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final TodoService todoService;

    public ApiController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * Retrieve all Todos. Accessible by USER or ADMIN.
     */
    @GetMapping("/todos")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<Todo> getAllTodos() {
        return todoService.getAllTodos();
    }

    /**
     * Retrieve a specific Todo by ID. Accessible by USER or ADMIN.
     */
    @GetMapping("/todos/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Todo> getTodoById(@PathVariable Long id) {
        return todoService.getTodoById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new Todo. Accessible by USER or ADMIN.
     */
    @PostMapping("/todos")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Todo> createTodo(@RequestBody Todo todo) {
        Todo created = todoService.createTodo(todo);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing Todo. Accessible by USER or ADMIN.
     */
    @PutMapping("/todos/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Todo> updateTodo(@PathVariable Long id, @RequestBody Todo todoDetails) {
        Todo updated = todoService.updateTodo(id, todoDetails);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an existing Todo. Restricted to ADMIN only.
     */
    @DeleteMapping("/todos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get mutation logs. Restricted to ADMIN only.
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TodoLog> getLogs() {
        return todoService.getAllLogs();
    }

    /**
     * Clear all mutation logs. Restricted to ADMIN only.
     */
    @PostMapping("/logs/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clearLogs() {
        todoService.clearLogs();
        return ResponseEntity.ok().build();
    }

}
