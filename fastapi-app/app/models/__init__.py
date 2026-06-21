from sqlalchemy.ext.declarative import declarative_base

# The declarative base class for all SQLAlchemy ORM models.
# This serves the same purpose as the JPA entity mapping standard in Java (Spring Data JPA).
Base = declarative_base()

# Export models to simplify package-level imports (e.g. `from app.models import Todo`)
from .todo import Todo
from .todo_log import TodoLog
