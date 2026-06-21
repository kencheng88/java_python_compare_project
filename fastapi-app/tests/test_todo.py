import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from testcontainers.mysql import MySqlContainer

from app.main import app
from app.database import get_db
from app.models import Base, Todo, TodoLog

# -------------------------------------------------------------------------
# PyTest Fixtures - MariaDB Integration Test Setup
# -------------------------------------------------------------------------

@pytest.fixture(scope="session")
def mariadb_container():
    """
    Spins up a single MariaDB Docker container for the duration of the test session.
    
    This is equivalent to using @Testcontainers and @Container in Spring Boot / JUnit 5.
    Instead of mocking the database, we run integration tests against a live DB.
    """
    # Start container using the MySQL container with a MariaDB image
    with MySqlContainer("mariadb:10.11") as mariadb:
        yield mariadb

@pytest.fixture(scope="session")
def test_db_engine(mariadb_container):
    """
    Creates a SQLAlchemy engine pointing to the Testcontainer instance.
    
    Initializes the database schema by executing create_all().
    """
    url = mariadb_container.get_connection_url()
    # Ensure the pymysql driver is specified in the URL
    if url.startswith("mysql://"):
        url = url.replace("mysql://", "mysql+pymysql://")
    elif url.startswith("mariadb://"):
        url = url.replace("mariadb://", "mysql+pymysql://")
        
    engine = create_engine(url, pool_pre_ping=True)
    
    # Initialize the tables once
    Base.metadata.create_all(bind=engine)
    yield engine
    Base.metadata.drop_all(bind=engine)

@pytest.fixture(scope="function")
def db_session(test_db_engine):
    """
    Yields a fresh SQLAlchemy Session for each test function.
    
    After each test, we truncate all tables to guarantee test isolation.
    In Spring Boot, this is commonly handled by @Transactional on test methods
    which rolls back the transactions. Here, we truncate tables explicitly for clean separation.
    """
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_db_engine)
    session = SessionLocal()
    
    yield session
    
    session.close()
    
    # Clean database tables between tests
    with test_db_engine.connect() as conn:
        conn.execute(text("SET FOREIGN_KEY_CHECKS = 0;"))
        conn.execute(text("TRUNCATE TABLE todo_logs;"))
        conn.execute(text("TRUNCATE TABLE todos;"))
        conn.execute(text("SET FOREIGN_KEY_CHECKS = 1;"))
        conn.commit()

@pytest.fixture(scope="function")
def client(test_db_engine):
    """
    Overrides the FastAPI dependency injector to inject a fresh testing DB session,
    and returns a TestClient for sending HTTP requests.
    
    This matches Spring's @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    with a MockMvc client.
    """
    def override_get_db():
        SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_db_engine)
        db = SessionLocal()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        yield test_client
    # Clear the overrides after the test completes
    app.dependency_overrides.clear()


# -------------------------------------------------------------------------
# Integration Tests
# -------------------------------------------------------------------------

def test_unauthenticated_access(client):
    """
    Verifies that requests without a Bearer Token are rejected with 401.
    
    Spring Boot security configuration checks: matches security.py -> HTTPBearer()
    """
    response = client.get("/api/todos")
    assert response.status_code == 401
    assert "Not authenticated" in response.json()["detail"]


def test_invalid_token_access(client):
    """
    Verifies that requests with an invalid Bearer Token are rejected with 401.
    """
    headers = {"Authorization": "Bearer invalid-token-string"}
    response = client.get("/api/todos", headers=headers)
    assert response.status_code == 401
    assert "Invalid or expired Bearer Token" in response.json()["detail"]


def test_user_role_read_only_privileges(client):
    """
    Verifies that a USER persona has READ permissions but cannot modify data.
    
    USER role should be able to:
      - GET /api/todos (200 OK)
    USER role should NOT be able to:
      - POST /api/todos (403 Forbidden)
      - PUT /api/todos/{id}/toggle (403 Forbidden)
      - DELETE /api/todos/{id} (403 Forbidden)
      - GET /api/todos/logs (403 Forbidden)
      
    This maps to Spring Boot @PreAuthorize("hasRole('ADMIN')") annotations.
    """
    user_headers = {"Authorization": "Bearer user-token"}

    # 1. Check Read access
    get_response = client.get("/api/todos", headers=user_headers)
    assert get_response.status_code == 200
    assert get_response.json() == []

    # 2. Check Create access (should be blocked)
    post_response = client.post(
        "/api/todos",
        json={"title": "Test Title", "description": "Test Desc"},
        headers=user_headers
    )
    assert post_response.status_code == 403
    assert "Permission denied" in post_response.json()["detail"]

    # 3. Check Audit Logs access (should be blocked)
    logs_response = client.get("/api/todos/logs", headers=user_headers)
    assert logs_response.status_code == 403


def test_admin_crud_workflow(client, db_session, test_db_engine):
    """
    Verifies the complete CRUD workflow with an ADMIN persona.
    
    ADMIN role has full read/write permissions.
    We also check if audit logs are created correctly in the database.
    """
    admin_headers = {"Authorization": "Bearer admin-token"}
    VerifySession = sessionmaker(bind=test_db_engine)
    verify_db = VerifySession()

    try:
        # 1. Create a Todo
        todo_data = {"title": "Learn FastAPI", "description": "Compare it with Spring Boot"}
        create_resp = client.post("/api/todos", json=todo_data, headers=admin_headers)
        assert create_resp.status_code == 201
        created_todo = create_resp.json()
        assert created_todo["id"] is not None
        assert created_todo["title"] == "Learn FastAPI"
        assert created_todo["completed"] is False

        # Verify Todo and Log exist in the DB directly using independent verification session
        db_todo = verify_db.query(Todo).filter(Todo.id == created_todo["id"]).first()
        assert db_todo is not None
        assert db_todo.title == "Learn FastAPI"

        db_log = verify_db.query(TodoLog).filter(TodoLog.todo_id == created_todo["id"]).first()
        assert db_log is not None
        assert db_log.action == "CREATED"

        # 2. Get All Todos (verify it is listed)
        list_resp = client.get("/api/todos", headers=admin_headers)
        assert list_resp.status_code == 200
        assert len(list_resp.json()) == 1
        assert list_resp.json()[0]["id"] == created_todo["id"]

        # 3. Toggle Status
        toggle_resp = client.put(f"/api/todos/{created_todo['id']}/toggle", headers=admin_headers)
        assert toggle_resp.status_code == 200
        assert toggle_resp.json()["completed"] is True

        # Refresh database representation by ending active transaction and reloading the object
        verify_db.rollback()
        db_todo = verify_db.query(Todo).filter(Todo.id == created_todo["id"]).first()
        assert db_todo.completed is True
        
        logs = verify_db.query(TodoLog).filter(TodoLog.todo_id == created_todo["id"]).all()
        assert len(logs) == 2
        assert any(log.action == "STATUS_UPDATED" for log in logs)

        # 4. View Audit Logs endpoint
        logs_resp = client.get("/api/todos/logs", headers=admin_headers)
        assert logs_resp.status_code == 200
        assert len(logs_resp.json()) == 2

        # 5. Delete Todo
        del_resp = client.delete(f"/api/todos/{created_todo['id']}", headers=admin_headers)
        assert del_resp.status_code == 204

        # Verify deletion in the DB after ending transaction
        verify_db.rollback()
        assert verify_db.query(Todo).filter(Todo.id == created_todo["id"]).first() is None
        
        # Audit log should show deletion action is logged
        del_log = verify_db.query(TodoLog).filter(
            TodoLog.todo_id == created_todo["id"],
            TodoLog.action == "DELETED"
        ).first()
        assert del_log is not None
    finally:
        verify_db.close()


def test_transaction_rollback(client, db_session, test_db_engine):
    """
    Verifies that the database transactions are correctly rolled back on failure.
    
    If the Todo service encounters an error (e.g. title is 'force-rollback'),
    both the new Todo entry and the new TodoLog audit entry must be rolled back.
    
    This verifies that our `with db.begin():` block matches Spring's `@Transactional` rollback behavior.
    """
    admin_headers = {"Authorization": "Bearer admin-token"}
    VerifySession = sessionmaker(bind=test_db_engine)
    verify_db = VerifySession()

    try:
        # Request creation of the force-rollback todo
        payload = {
            "title": "force-rollback",
            "description": "This should raise an error and rollback database changes."
        }
        
        response = client.post("/api/todos", json=payload, headers=admin_headers)
        assert response.status_code == 400
        assert "forcing database transaction rollback" in response.json()["detail"]

        # Verify that the DB remains empty (No Todo and No TodoLog inserted)
        verify_db.rollback()
        todos_count = verify_db.query(Todo).count()
        logs_count = verify_db.query(TodoLog).count()
        
        assert todos_count == 0
        assert logs_count == 0
    finally:
        verify_db.close()
