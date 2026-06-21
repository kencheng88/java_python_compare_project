from sqlalchemy import Column, Integer, String, DateTime, func
from . import Base

class TodoLog(Base):
    """
    SQLAlchemy ORM Model representing the 'todo_logs' table.
    
    This is equivalent to com.example.todo.model.TodoLog.java in the Spring Boot project.
    It tracks transactional audit logs of CRUD operations performed on the todos table.
    """
    __tablename__ = "todo_logs"

    id = Column(Integer, primary_key=True, autoincrement=True)
    todo_id = Column(Integer, nullable=False)
    action = Column(String(50), nullable=False)  # 'CREATED', 'STATUS_UPDATED', 'DELETED'
    details = Column(String(255), nullable=True)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
