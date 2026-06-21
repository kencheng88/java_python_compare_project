# Exception Handling Comparison: Centralized vs. Local try-catch

This document provides a side-by-side comparison of **Local Exception Handling (try-catch)** and **Centralized Global Exception Handling** in both Python (FastAPI) and Java (Spring Boot).

---

## 🐍 Python (FastAPI) Comparison

### 1. Local try-except Pattern (Before Refactoring)
In this pattern, each controller endpoint handles exceptions individually using a local `try-except` block and manually wraps the error in a FastAPI `HTTPException`.

```python
# fastapi-app/app/controllers/api_controller.py (LEGACY)

@router.post("", response_model=TodoResponse, status_code=status.HTTP_201_CREATED)
def create_todo(todo_in: TodoCreate, db: Session = Depends(get_db)):
    try:
        # Business logic can raise ValueError or other runtime errors
        return todo_service.create_todo(db, todo_in.title, todo_in.description)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/{todo_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_todo(todo_id: int, db: Session = Depends(get_db)):
    try:
        todo_service.delete_todo(db, todo_id)
        return None
    except KeyError as e:
        raise HTTPException(status_code=404, detail=str(e))
```

### 2. Centralized Exception Handler Pattern (After Refactoring)
Here, endpoints are clean of error-handling boilerplate. Exceptions bubble up from the service layer to global exception handlers registered on the FastAPI app instance.

```python
# fastapi-app/app/controllers/api_controller.py (CURRENT)

@router.post("", response_model=TodoResponse, status_code=status.HTTP_201_CREATED)
def create_todo(todo_in: TodoCreate, db: Session = Depends(get_db)):
    # Let custom exceptions bubble up directly
    return todo_service.create_todo(db, todo_in.title, todo_in.description)
```

```python
# fastapi-app/app/exceptions.py (CURRENT)

class TodoTransactionError(Exception):
    pass

class TodoNotFoundError(Exception):
    pass

def register_exception_handlers(app):
    @app.exception_handler(TodoTransactionError)
    async def todo_transaction_error_handler(request: Request, exc: TodoTransactionError):
        return JSONResponse(
            status_code=400,
            content={"status": "error", "detail": exc.message}
        )

    @app.exception_handler(TodoNotFoundError)
    async def todo_not_found_error_handler(request: Request, exc: TodoNotFoundError):
        return JSONResponse(
            status_code=404,
            content={"status": "error", "detail": exc.message}
        )
```

---

## ☕ Java (Spring Boot) Comparison

### 1. Local Controller @ExceptionHandler Pattern (Before Refactoring)
In this pattern, exceptions are thrown from the service layer and captured locally within the controller class using `@ExceptionHandler` methods.

```java
// springboot-app/src/main/java/com/example/todo/controller/ApiController.java (LEGACY)

@RestController
@RequestMapping("/api")
public class ApiController {
    // ... endpoints call service ...

    // Local exception handlers inside the Controller class:
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRollbackAndRuntime(RuntimeException ex) {
        // Warning: This catches AccessDeniedException from Spring Security!
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            throw ex; // Re-throw to avoid swallowing security exception
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
```

### 2. Centralized @RestControllerAdvice Pattern (After Refactoring)
In this pattern, all controllers remain entirely clean of exception-handling code. A separate global advice class handles specific custom exceptions across the entire application context.

```java
// springboot-app/src/main/java/com/example/todo/controller/ApiController.java (CURRENT)

@RestController
@RequestMapping("/api")
public class ApiController {
    // No @ExceptionHandler methods inside the controller anymore
}
```

```java
// springboot-app/src/main/java/com/example/todo/exception/GlobalExceptionHandler.java (CURRENT)

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TodoTransactionException.class)
    public ResponseEntity<Map<String, Object>> handleTodoTransactionException(TodoTransactionException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("detail", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(TodoNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTodoNotFoundException(TodoNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("detail", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
```

---

## 📊 Comparative Analysis

| Dimension | Local Exception Handling (try-catch / Local `@ExceptionHandler`) | Centralized Exception Handling (Global Handlers / `@RestControllerAdvice`) |
| :--- | :--- | :--- |
| **Code Duplication** | **High**: Boilerplate error handling is copied across many routes or controllers. | **Low**: Code mappings from exceptions to HTTP codes are defined once globally. |
| **Separation of Concerns** | **Poor**: Controllers are cluttered with translation logic (converting exceptions to HTTP responses). | **Clean**: Controllers focus strictly on routing; mapping is handled in dedicated advice classes. |
| **Security Risk** | **High**: Generic handlers (like catching `RuntimeException` in Spring Boot) can accidentally intercept and swallow security authorization exceptions. | **Low**: Handlers explicitly map custom domain-specific exceptions, leaving system exceptions to bubble up. |
| **Granularity** | **Flexible**: Easily return custom payloads or logs unique to a specific endpoint function. | **Systematic**: Standardizes the API error payload format (e.g. always returning `{"status": "error", "detail": "..."}`). |

### Recommended Best Practice
Use **Centralized Exception Handling** for standard application error flows (such as 400 Bad Request, 404 Not Found, 409 Conflict). Only use **Local try-catch** blocks when a specific endpoint requires a custom side-effect (e.g., fallback recovery, logging specific diagnostic data) that is unique to that route.
