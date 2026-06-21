from sqlalchemy.orm import Session
from app.models.todo import Todo
from app.models.todo_log import TodoLog
from app.repositories import TodoRepository, TodoLogRepository
from app.exceptions import TodoTransactionError, TodoNotFoundError

class TodoService:
    """
    Business Logic Service Layer for managing Todo items and writing audit logs.
    
    This corresponds to com.example.todo.service.TodoService in the Spring Boot project.
    In Spring Boot, we annotate methods or the class with `@Transactional` to manage transactions.
    In FastAPI/SQLAlchemy, we use the `db.begin()` context manager to define transaction boundaries.
    """

    def __init__(self):
        self.todo_repo = TodoRepository()
        self.log_repo = TodoLogRepository()

    def get_all_todos(self, db: Session) -> list[Todo]:
        """Fetch all todos using the repository."""
        return self.todo_repo.find_all(db)

    def get_recent_logs(self, db: Session, limit: int = 50) -> list[TodoLog]:
        """Fetch recent transaction logs."""
        return self.log_repo.find_recent(db, limit)

    def create_todo(self, db: Session, title: str, description: str | None) -> Todo:
        """
        Creates a Todo and logs the action in a single database transaction.
        
        If an exception occurs within the `with db.begin()` block, SQLAlchemy
        automatically issues a ROLLBACK to the database. Otherwise, it issues a COMMIT.
        """
        # Defensive check
        if not title or not title.strip():
            raise ValueError("Todo title cannot be empty")

        todo = Todo(title=title, description=description)

        # Transaction boundary starts here
        with db.begin():
            # 1. Save the new Todo
            self.todo_repo.save(db, todo)
            db.flush()  # Generates the auto-increment ID in the database session
            
            # 2. Write the audit log
            log = TodoLog(
                todo_id=todo.id,
                action="CREATED",
                details=f"Created todo '{todo.title}'"
            )
            self.log_repo.save(db, log)

            # Simulated failure for testing transaction rollback
            if title == "force-rollback":
                raise TodoTransactionError("Simulated failure: forcing database transaction rollback")
        
        # Transaction is committed and connection closed here (outside the block)
        return todo

    def toggle_todo_status(self, db: Session, todo_id: int) -> Todo:
        """Toggles the completed status of a Todo and logs the action in a transaction."""
        with db.begin():
            todo = self.todo_repo.find_by_id(db, todo_id)
            if not todo:
                raise TodoNotFoundError(f"Todo with ID {todo_id} not found")

            # Toggle status
            todo.completed = not todo.completed
            self.todo_repo.save(db, todo)

            action_desc = "COMPLETED" if todo.completed else "INCOMPLETE"
            log = TodoLog(
                todo_id=todo.id,
                action="STATUS_UPDATED",
                details=f"Marked todo '{todo.title}' as {action_desc}"
            )
            self.log_repo.save(db, log)

        return todo

    def delete_todo(self, db: Session, todo_id: int) -> None:
        """Deletes a Todo and logs the action in a transaction."""
        with db.begin():
            todo = self.todo_repo.find_by_id(db, todo_id)
            if not todo:
                raise TodoNotFoundError(f"Todo with ID {todo_id} not found")

            self.todo_repo.delete(db, todo)

            log = TodoLog(
                todo_id=todo_id,
                action="DELETED",
                details=f"Deleted todo '{todo.title}'"
            )
            self.log_repo.save(db, log)
