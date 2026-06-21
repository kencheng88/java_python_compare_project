# Backend Comparison Environment: Todo List with Audit Logs, Security, OpenAPI & MCP Customization (FastAPI vs. Spring Boot)

This project provides a comparison environment featuring a Python (FastAPI) web application and a Java (Spring Boot) web application. Both applications implement a **Todo List Management** system with **Audit Logs**, **Authentication/Authorization**, **Human OpenAPI (Swagger)**, and a **Dedicated MCP-Tailored OpenAPI Endpoint** for LLM tool consumption.

---

## MCP OpenAPI Customization Design

The Model Context Protocol (MCP) gateway parses OpenAPI JSON files to expose endpoints as "tools" to an LLM. Since LLMs require highly detailed prompt-like descriptions to operate parameters and security correctly, we expose a separate OpenAPI schema with programmatically modified description fields.

### 1. Spring Boot (Java) MCP OpenAPI Customization
We define a custom `GroupedOpenApi` bean in `config/OpenApiConfig.java`. It targets the same endpoints but applies an `OpenApiCustomizer` to inject LLM-oriented tool descriptions.
*   **Java Configuration**:
    ```java
    @Bean
    public GroupedOpenApi mcpOpenApi() {
        return GroupedOpenApi.builder()
            .group("mcp")
            .pathsToMatch("/api/todos/**")
            .addOpenApiCustomizer(openApi -> {
                openApi.getInfo().setTitle("Todo List API for MCP Gateway");
                openApi.getInfo().setDescription("Tailored tool descriptions for LLM agents.");
                
                // Programmatically rewrite description for creating a Todo
                var pathItem = openApi.getPaths().get("/api/todos");
                if (pathItem != null && pathItem.getPost() != null) {
                    pathItem.getPost().setDescription(
                        "TOOL INSTRUCTION: Use this tool to create a new Todo item. " +
                        "Note: The title cannot be empty. " +
                        "REQUIRED HEADER: 'Authorization: Bearer admin-token' must be supplied."
                    );
                }
            })
            .build();
    }
    ```
*   **Access Point**: The MCP gateway will read the customized JSON from: `http://localhost:8080/v3/api-docs/mcp`.

### 2. FastAPI (Python) MCP OpenAPI Customization
In FastAPI, we dynamically build the OpenAPI schema and programmatically edit the dictionary before serving it. We register a custom route `/api/mcp/openapi.json`.
*   **Python Router (`app/controllers/debug_controller.py`)**:
    ```python
    from fastapi import FastAPI
    from fastapi.openapi.utils import get_openapi

    def register_mcp_openapi(app: FastAPI):
        @app.get("/api/mcp/openapi.json", include_in_schema=False)
        def get_mcp_openapi():
            # 1. Generate default OpenAPI schema
            schema = get_openapi(
                title="Todo List API for MCP Gateway",
                version="1.0.0",
                description="Tailored tool descriptions for LLM agents.",
                routes=app.routes,
            )
            
            # 2. Modify descriptions dynamically for MCP consumption
            if "/api/todos" in schema.get("paths", {}):
                post_op = schema["paths"]["/api/todos"].get("post")
                if post_op:
                    post_op["description"] = (
                        "TOOL INSTRUCTION: Use this tool to create a new Todo item. "
                        "Note: The title cannot be empty. "
                        "REQUIRED HEADER: 'Authorization: Bearer admin-token' must be supplied."
                    )
            return schema
    ```
*   **Access Point**: The MCP gateway will read the customized JSON from: `http://localhost:8000/api/mcp/openapi.json`.

---

## Database Schema Design (Two Tables)

### 1. `todos` Table (Stores todo items)
| Column Name   | Data Type     | Constraints / Attributes                          | Description                               |
|:--------------|:--------------|:--------------------------------------------------|:------------------------------------------|
| `id`          | `BIGINT`      | `AUTO_INCREMENT`, `PRIMARY KEY`                   | Unique identifier for each todo item.     |
| `title`       | `VARCHAR(255)`| `NOT NULL`                                        | The title of the todo task.               |
| `description` | `TEXT`        | `NULL`                                            | Optional longer description/notes.        |
| `completed`   | `BOOLEAN`     | `NOT NULL`, `DEFAULT FALSE`                       | Completion status (`true`/`false`).       |
| `created_at`  | `TIMESTAMP`   | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP`           | Timestamp when the todo was created.      |
| `updated_at`  | `TIMESTAMP`   | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP ON UPDATE` | Timestamp when the todo was last updated. |

### 2. `todo_logs` Table (Audit log for transaction tracking)
| Column Name  | Data Type     | Constraints / Attributes                | Description                                        |
|:-------------|:--------------|:----------------------------------------|:---------------------------------------------------|
| `id`         | `BIGINT`      | `AUTO_INCREMENT`, `PRIMARY KEY`         | Unique identifier for the log entry.              |
| `todo_id`    | `BIGINT`      | `NOT NULL`                              | The ID of the associated todo item.                |
| `action`     | `VARCHAR(50)` | `NOT NULL`                              | Action taken: `CREATED`, `STATUS_UPDATED`, `DELETED`|
| `details`    | `VARCHAR(255)`| `NULL`                                  | Dynamic text describing the change.                |
| `created_at` | `TIMESTAMP`   | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Timestamp when the log entry was created.          |

### DDL (SQL)
```sql
CREATE TABLE todos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT DEFAULT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE todo_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    todo_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    details VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## Transaction Control Design

Whenever a Todo is manipulated, the change **and** the audit log entry must succeed together:

### 1. Spring Boot (Java) Transaction Control
We use Spring's `@Transactional` annotation on the `TodoService` class or specific methods.
*   **Success Scenario**:
    ```java
    @Transactional
    public Todo createTodo(Todo todo) {
        Todo savedTodo = todoRepository.save(todo);
        
        TodoLog log = new TodoLog(savedTodo.getId(), "CREATED", "Todo titled '" + savedTodo.getTitle() + "' was created.");
        todoLogRepository.save(log);
        
        return savedTodo;
    }
    ```

### 2. FastAPI (Python) Transaction Control
We use SQLAlchemy's transactional context manager (`Session.begin()`) in `TodoService`.
*   **Success Scenario**:
    ```python
    def create_todo(self, db: Session, todo_in: TodoCreate) -> Todo:
        with db.begin():  # Starts a transaction
            todo = Todo(title=todo_in.title, description=todo_in.description)
            db.add(todo)
            db.flush()  # Generates the auto-increment todo.id
            
            log = TodoLog(todo_id=todo.id, action="CREATED", details=f"Todo titled '{todo.title}' was created.")
            db.add(log)
            # The transaction automatically commits here if no exceptions occur
        return todo
    ```

---

## Diagnostics (Actuator vs. Custom Debug Router)

To compare operational capabilities, we expose the following endpoints:

| Feature / Goal         | Spring Boot Endpoint           | FastAPI Endpoint              | Implementation Details / Behavior |
|:-----------------------|:-------------------------------|:------------------------------|:----------------------------------|
| **Health Check**       | `/actuator/health`             | `/api/debug/health`           | Checks database connection status.|
| **Thread Dump**        | `/actuator/threaddump`         | `/api/debug/threaddump`       | Dumps active stacks & threads.    |
| **OOM Trigger**        | `/api/debug/oom`               | `/api/debug/oom`              | Induces OutOfMemory crash.        |

---

## Aligned Project Structures

```
D:\side_project\
1. docker-compose.yml           # Runs local MariaDB for development (Port 3406)
2. fastapi-app/
   ├── app/
   │   ├── main.py              # App entry point
   │   ├── config.py            # Settings (DB credentials)
   │   ├── security.py          # HTTPBearer authentication & verify_role dependencies
   │   ├── controllers/
   │   │   ├── web_controller.py
   │   │   ├── api_controller.py   # Secured via verify_role (USER/ADMIN Check)
   │   │   └── debug_controller.py # Handles /api/debug/health, /threaddump, /oom, /api/mcp/openapi.json
   │   ├── services/
   │   │   └── todo_service.py
   │   ├── repositories/
   │   │   ├── todo_repository.py
   │   │   └── todo_log_repository.py
   │   ├── models/
   │   │   ├── todo.py          # SQLAlchemy ORM Model
   │   │   └── todo_log.py      # SQLAlchemy ORM Model for Audit Log
   │   ├── static/
   │   │   └── style.css
   │   └── templates/
   │       └── index.html
   ├── tests/
   │   └── test_todo.py         # Pytest + testcontainers-python integration test
   └── requirements.txt         # fastapi, uvicorn, jinja2, sqlalchemy, pymysql, pytest, testcontainers
3. springboot-app/
   ├── pom.xml                  # Maven config (includes Security, Actuator, Springdoc, Data JPA, MariaDB, Testcontainers)
   └── src/
       ├── main/
       │   ├── java/
       │   │   └── com/
       │   │       └── example/
       │   │           └── todo/
       │   │               ├── TodoApplication.java
       │   │               ├── config/
       │   │               │   └── SecurityConfig.java   # Spring Security Filter Chain
       │   │               │   └── TokenAuthenticationFilter.java # Custom Bearer Filter
       │   │               │   └── OpenApiConfig.java    # Custom OpenAPI & MCP configuration
       │   │               ├── controller/
       │   │               │   ├── ApiController.java    # REST API endpoints
       │   │               │   └── DebugController.java  # Custom OOM trigger endpoint
       │   │               ├── service/
       │   │               │   └── TodoService.java
       │   │               ├── exception/
       │   │               │   └── GlobalExceptionHandler.java # REST global exception interceptor
       │   │               └── model/
       │   │                   ├── Todo.java
       │   │                   └── TodoLog.java
       │   └── resources/
       │       ├── application.properties               # Runs on Port 8080, Actuator and Springdoc configs
       │       └── templates/
       │           └── index.html                       # Frontend thymeleaf templates
```

---

## Verification Plan

### 1. Python FastAPI Setup & Running
To ensure package isolation, run the following commands in the `fastapi-app/` directory:
```powershell
# 1. Create a virtual environment
python -m venv venv

# 2. Activate the virtual environment (Windows PowerShell)
.\venv\Scripts\Activate.ps1

# 3. Upgrade pip and install dependencies
python -m pip install --upgrade pip
pip install -r requirements.txt
```

#### Running FastAPI Integration Tests
Make sure Docker is running on your machine, then run:
```powershell
$env:TESTCONTAINERS_RYUK_DISABLED="true"
.\venv\Scripts\pytest
```

#### Running FastAPI App Manually
Ensure the local MariaDB container is running (`docker compose up -d` at the root folder), then run:
```powershell
uvicorn app.main:app --reload --port 8000
```
*   Swagger UI: `http://localhost:8000/docs`
*   **MCP-Specific OpenAPI JSON**: `http://localhost:8000/api/mcp/openapi.json`

---

### 2. Java Spring Boot Setup & Running
Run the following commands in the `springboot-app/` directory:

#### Running Spring Boot Integration Tests
Make sure Docker is running, then run:
```powershell
$env:TESTCONTAINERS_RYUK_DISABLED="true"
mvn test
```

#### Running Spring Boot App Manually
Ensure the local MariaDB container is running (`docker compose up -d` at the root folder), then run:
```powershell
mvn spring-boot:run
```
*   Swagger UI: `http://localhost:8080/swagger-ui/index.html`
*   **MCP-Specific OpenAPI JSON**: `http://localhost:8080/v3/api-docs/mcp`
