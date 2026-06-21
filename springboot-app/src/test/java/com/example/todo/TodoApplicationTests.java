package com.example.todo;

import com.example.todo.model.Todo;
import com.example.todo.model.TodoLog;
import com.example.todo.repository.TodoLogRepository;
import com.example.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Spring Boot Todo application.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In Python/FastAPI, integration tests typically use {@code TestClient} or {@code httpx.AsyncClient}
 * combined with a test database (often SQLite in-memory or a Docker-based Postgres/MySQL container spun up
 * using pytest fixtures).
 * In Spring Boot, we use {@link SpringBootTest} with a random port to start the full application server context.
 * We use the {@link Testcontainers} library to boot a real MariaDB instance inside a Docker container,
 * ensuring that our tests run against the exact same database engine used in production. We dynamically
 * configure the Spring Datasource properties using {@link DynamicPropertySource}.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TodoApplicationTests {

    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:10.6");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private TodoLogRepository todoLogRepository;

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        todoLogRepository.deleteAll();
    }

    /**
     * Helper to build HTTP Headers with a bearer token.
     */
    private HttpHeaders getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    @Test
    void contextLoads() {
        assertTrue(mariadb.isRunning());
    }

    @Test
    void testSecurity_AnonymousAccessDenied() {
        // GET /api/todos with no token should return 403 Forbidden
        ResponseEntity<Object[]> response = restTemplate.getForEntity("/api/todos", Object[].class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testSecurity_UserRoleAccess() {
        // GET /api/todos with user-token should succeed (200 OK)
        HttpHeaders headers = getHeaders("user-token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Todo[]> response = restTemplate.exchange("/api/todos", HttpMethod.GET, entity, Todo[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // POST /api/todos with user-token should succeed (201 Created)
        Todo newTodo = new Todo("User Task", "Created by User", false);
        HttpEntity<Todo> postEntity = new HttpEntity<>(newTodo, headers);
        ResponseEntity<Todo> postResponse = restTemplate.exchange("/api/todos", HttpMethod.POST, postEntity, Todo.class);
        assertEquals(HttpStatus.CREATED, postResponse.getStatusCode());
        assertNotNull(postResponse.getBody().getId());

        // GET /api/logs with user-token should fail (403 Forbidden)
        ResponseEntity<String> logResponse = restTemplate.exchange("/api/logs", HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.FORBIDDEN, logResponse.getStatusCode());

        // DELETE /api/todos/{id} with user-token should fail (403 Forbidden)
        Long todoId = postResponse.getBody().getId();
        ResponseEntity<Void> deleteResponse = restTemplate.exchange("/api/todos/" + todoId, HttpMethod.DELETE, entity, Void.class);
        assertEquals(HttpStatus.FORBIDDEN, deleteResponse.getStatusCode());
    }

    @Test
    void testSecurity_AdminRoleAccess() {
        // First, create a Todo
        Todo todo = new Todo("Admin Task", "Created by Admin", false);
        todo = todoRepository.save(todo);

        HttpHeaders headers = getHeaders("admin-token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // GET /api/logs with admin-token should succeed
        ResponseEntity<TodoLog[]> logResponse = restTemplate.exchange("/api/logs", HttpMethod.GET, entity, TodoLog[].class);
        assertEquals(HttpStatus.OK, logResponse.getStatusCode());

        // DELETE /api/todos/{id} with admin-token should succeed
        ResponseEntity<Void> deleteResponse = restTemplate.exchange("/api/todos/" + todo.getId(), HttpMethod.DELETE, entity, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        assertFalse(todoRepository.existsById(todo.getId()));
    }

    @Test
    void testTransactions_CreateAndLogSucceeds() {
        HttpHeaders headers = getHeaders("user-token");
        Todo todo = new Todo("Valid Task", "This task is valid and should be saved", false);
        HttpEntity<Todo> entity = new HttpEntity<>(todo, headers);

        ResponseEntity<Todo> response = restTemplate.exchange("/api/todos", HttpMethod.POST, entity, Todo.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Check if both Todo and TodoLog are saved in the database
        List<Todo> todos = todoRepository.findAll();
        List<TodoLog> logs = todoLogRepository.findAll();

        assertEquals(1, todos.size());
        assertEquals("Valid Task", todos.get(0).getTitle());

        assertEquals(1, logs.size());
        assertEquals("CREATE", logs.get(0).getAction());
        assertTrue(logs.get(0).getDetails().contains("Valid Task"));
    }

    @Test
    void testTransactions_ForceRollbackRevertsEverything() {
        HttpHeaders headers = getHeaders("user-token");
        
        // This title triggers the RuntimeException inside TodoService.createTodo
        Todo rollbackTodo = new Todo("force-rollback", "Should cause transaction rollback", false);
        HttpEntity<Todo> entity = new HttpEntity<>(rollbackTodo, headers);

        ResponseEntity<String> response = restTemplate.exchange("/api/todos", HttpMethod.POST, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Forced transaction rollback triggered"));

        // Verify that the transaction rolled back.
        // Neither the Todo nor the TodoLog should be persisted in the database!
        List<Todo> todos = todoRepository.findAll();
        List<TodoLog> logs = todoLogRepository.findAll();

        assertTrue(todos.isEmpty(), "Todo table should be empty due to transaction rollback!");
        assertTrue(logs.isEmpty(), "TodoLog table should be empty due to transaction rollback!");
    }
}
