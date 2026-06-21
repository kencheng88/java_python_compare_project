# Key Discussion & Design Decision Notes

This document captures the key engineering problems, design tradeoffs, and technical solutions discussed and implemented during the development of this side-by-side backend comparison project.

---

## 1. Environment & Database Infrastructure (Windows Hyper-V Port Exclusion)
*   **Problem**: In standard Docker setups, MariaDB runs on host port `3306` or `3307`. However, on Windows machines utilizing Hyper-V (WSL2), these ports are frequently reserved inside the Windows `excludedportrange` (reserved system network ports). Attempts to bind containers to `3306` fail with generic networking socket errors.
*   **Solution**: Mapped the host port to `3406` (container port remains `3306` internally). All application configurations in Python (`app/config.py`) and Java (`application.properties`) were updated to direct database traffic to host port `3406`.

---

## 2. Integration Testing with Testcontainers (Ryuk Socket Privilege on Windows)
*   **Problem**: Both projects leverage **Testcontainers** to spin up an ephemeral MariaDB instance during test phases. On Windows, Testcontainers spins up a sidecar helper container called "Ryuk" (responsible for resource reaping and cleanup). Due to Windows network socket privilege restrictions, Ryuk fails to communicate, causing test suites to freeze or fail initialization.
*   **Solution**: Disabled the Ryuk sidecar process during test execution. In PowerShell, this is configured prior to running test commands:
    ```powershell
    $env:TESTCONTAINERS_RYUK_DISABLED="true"
    ```
    This allows the test containers to start up and run queries successfully.

---

## 3. SQLAlchemy Shared Session Contamination (FastAPI pytest)
*   **Problem**: In pytest, sharing a single active SQLAlchemy Session across multiple endpoint requests within a test led to the error `InvalidRequestError: A transaction is already begun`. This occurred because FastAPI's dependency injection was serving a dirty session that had uncommitted transactions or lock-outs.
*   **Solution**:
    *   Updated the test client override dependency `override_get_db()` to yield a fresh database session per HTTP call and properly close it in a `finally` block.
    *   Instantiated an independent verification session (`verify_db`) locally in each test function, ensuring transaction boundaries are completely isolated between HTTP clients and direct DB assertions.

---

## 4. Spring Security Exception Swallowing
*   **Problem**: In Spring Boot, when a catch-all global advice `@ExceptionHandler(RuntimeException.class)` is registered, it intercepts Spring Security's authorization failures (e.g. `AccessDeniedException`), because they are subclasses of `RuntimeException`. This incorrectly converted HTTP `403 Forbidden` statuses into `400 Bad Request` API error responses, breaking RBAC contract validation.
*   **Solution**:
    *   Replaced catch-all exception blocks.
    *   Defined domain-specific custom exceptions (`TodoTransactionException` and `TodoNotFoundException`).
    *   Configured the global `@RestControllerAdvice` to handle only these specific domain errors, allowing Spring Security's `AccessDeniedException` to bubble up to the security framework filters naturally, resulting in the correct `403 Forbidden` response.

---

## 5. Centralized Exception Handling vs. Local try-catch
*   **Problem**: Local `try-except` (Python) and `try-catch` (Java) statements inside individual controller mapping methods duplicate boilerplate code and decouple exception mapping from HTTP status codes.
*   **Solution**:
    *   **FastAPI (Python)**: Created `app/exceptions.py` registering global handler functions via `@app.exception_handler(...)` mapping `TodoTransactionError`, `TodoNotFoundError`, and `ValueError` to structured JSON responses containing both `detail` and `message` fields.
    *   **Spring Boot (Java)**: Configured a central `GlobalExceptionHandler` annotated with `@RestControllerAdvice` and `@ExceptionHandler(...)` to process equivalent Java custom exceptions.
    *   In both stacks, controller files now cleanly delegate execution to services, letting exception bubbles transform automatically into HTTP error models.

---

## 6. MCP OpenAPI Schema Customization
*   **Context**: LLMs parsing API documentation via Model Context Protocol (MCP) gateways need highly structured, explicit prompt-like parameters and header guidelines (e.g. specific token credentials, strict validation rules) to avoid runtime execution errors.
*   **Implementation details**:
    *   **FastAPI**: Exposes a separate route `/api/mcp/openapi.json` which dynamically pulls the baseline schema via `get_openapi(...)` and overrides description keys programmatically.
    *   **Spring Boot**: Configures a dedicated `GroupedOpenApi` bean mapping to `/v3/api-docs/mcp` and applies an `OpenApiCustomizer` to enrich path descriptions for LLMs.
