from sqlalchemy import Column, Integer, String, Text, Boolean, DateTime, func
from . import Base

class Todo(Base):
    """
    SQLAlchemy ORM Model representing the 'todos' table.
    
    This is equivalent to com.example.todo.model.Todo.java in the Spring Boot project.
    In Spring Boot, Hibernate maps Java classes to database tables via `@Entity`.
    In FastAPI, SQLAlchemy uses `declarative_base` and `Column` classes.
    """
    __tablename__ = "todos"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    completed = Column(Boolean, nullable=False, default=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
