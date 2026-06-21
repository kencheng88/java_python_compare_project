from sqlalchemy.orm import Session
from app.models.todo_log import TodoLog

class TodoLogRepository:
    """
    Data Access Object (DAO) for the 'todo_logs' table.
    
    This acts similarly to com.example.todo.repository.TodoLogRepository in Spring Boot.
    """

    def save(self, db: Session, todo_log: TodoLog) -> TodoLog:
        """Save a new audit log entry."""
        db.add(todo_log)
        return todo_log

    def find_recent(self, db: Session, limit: int = 50) -> list[TodoLog]:
        """Fetch the most recent audit logs for administration or auditing purposes."""
        return db.query(TodoLog).order_by(TodoLog.created_at.desc()).limit(limit).all()
