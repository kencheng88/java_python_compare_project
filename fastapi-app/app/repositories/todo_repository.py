from sqlalchemy.orm import Session
from app.models.todo import Todo

class TodoRepository:
    """
    Data Access Object (DAO) for the 'todos' table.
    
    This acts similarly to com.example.todo.repository.TodoRepository in Spring Boot,
    which extends JpaRepository. In Java, JpaRepository handles basic CRUD automatically.
    In Python, we explicitly define these query operations using the SQLAlchemy Session.
    """

    def find_all(self, db: Session) -> list[Todo]:
        """Fetch all todo items ordered by creation time descending."""
        return db.query(Todo).order_by(Todo.created_at.desc()).all()

    def find_by_id(self, db: Session, todo_id: int) -> Todo | None:
        """Find a todo item by its primary key ID."""
        return db.query(Todo).filter(Todo.id == todo_id).first()

    def save(self, db: Session, todo: Todo) -> Todo:
        """Save (insert or update) a todo item in the session."""
        db.add(todo)
        return todo

    def delete(self, db: Session, todo: Todo) -> None:
        """Delete a todo item from the session."""
        db.delete(todo)
