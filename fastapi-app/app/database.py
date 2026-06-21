from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.config import DATABASE_URL

# Create the SQLAlchemy Engine
# This handles the connection pool, similar to HikariCP in Spring Boot.
engine = create_engine(
    DATABASE_URL,
    pool_pre_ping=True,      # Checks if connections are alive before using them
    pool_recycle=3600,       # Recycles connections older than 1 hour
)

# Create the SessionLocal class
# Each instance represents a database session (similar to JPA EntityManager)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db():
    """
    Dependency to yield a database session per HTTP request.
    
    This matches the 'Open Session In View' pattern used by Spring Boot by default.
    The session is closed automatically once the HTTP request completes.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
