import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.openapi.utils import get_openapi

from app.database import engine
from app.models import Base
from app.controllers import api_controller, debug_controller, web_controller
from app.controllers.debug_controller import register_mcp_openapi
from app.exceptions import register_exception_handlers

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Context manager to handle startup and shutdown events.
    
    This replaces the legacy @app.on_event("startup") decorator.
    Here, we initialize the database tables on startup, which is equivalent
    to Spring Boot Hibernate's 'spring.jpa.hibernate.ddl-auto=update' behavior.
    """
    # Create all database tables defined by SQLAlchemy models if they do not exist
    Base.metadata.create_all(bind=engine)
    yield
    # Shutdown operations (e.g. closing pools) can be placed here if needed

# Initialize the FastAPI app
app = FastAPI(
    title="Todo List API & Web Dashboard",
    description=(
        "FastAPI comparison project highlighting Python MVC patterns, "
        "declarative SQLAlchemy ORM, transaction control, security filters, "
        "and diagnostics equivalent to Spring Boot actuators."
    ),
    version="1.0.0",
    lifespan=lifespan
)

# Register routers (controllers)
# Matches Spring Boot Router/Controller mapping annotations
app.include_router(web_controller.router)
app.include_router(api_controller.router)
app.include_router(debug_controller.router)

# Mount static files (stylesheets, images, frontend client scripts)
# Equivalent to Spring Boot's resource handler registry for /static/**
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
app.mount("/static", StaticFiles(directory=os.path.join(BASE_DIR, "static")), name="static")

# Register the custom Model Context Protocol (MCP) OpenAPI endpoint
# This generates custom prompt instructions on schemas specifically tailored for LLM agents.
register_mcp_openapi(app)

# Register the global exception handlers
register_exception_handlers(app)
