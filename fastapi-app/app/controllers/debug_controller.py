import sys
import traceback
from fastapi import APIRouter, Depends, HTTPException, FastAPI
from fastapi.openapi.utils import get_openapi
from sqlalchemy import text
from sqlalchemy.orm import Session
from app.database import get_db

router = APIRouter(prefix="/api/debug", tags=["Diagnostics API"])

@router.get(
    "/health",
    summary="Health check endpoint",
    description="Validates the API status and tests the connection to the database. Similar to Spring Boot Actuator's /actuator/health."
)
def health_check(db: Session = Depends(get_db)):
    """Checks database connectivity to verify application health."""
    try:
        # Execute a trivial database check
        db.execute(text("SELECT 1"))
        return {
            "status": "UP",
            "details": {
                "database": "MariaDB connected successfully",
                "python_version": sys.version
            }
        }
    except Exception as e:
        raise HTTPException(
            status_code=503,
            detail={
                "status": "DOWN",
                "details": {
                    "database": f"Database check failed: {str(e)}"
                }
            }
        )

@router.get(
    "/threaddump",
    summary="Generate active thread stack traces",
    description="Dumps current stack traces for all active Python threads. Similar to Spring Boot Actuator's /actuator/threaddump."
)
def get_thread_dump():
    """Iterates through all active frames in the interpreter and returns their stack traces."""
    thread_dump = {}
    for thread_id, frame in sys._current_frames().items():
        thread_dump[str(thread_id)] = traceback.format_stack(frame)
    return {
        "active_threads_count": len(thread_dump),
        "threads": thread_dump
    }

@router.get(
    "/oom",
    summary="Trigger OutOfMemory crash",
    description="Allocates infinite blocks of memory in a loop. Causes OS OOM Killer termination or MemoryError."
)
def trigger_oom():
    """Forces the python interpreter to consume all available memory in a tight loop."""
    memory_waster = []
    # Loop indefinitely to consume system resources
    while True:
        # Append 100MB chunks of null bytes
        memory_waster.append(bytes(1024 * 1024 * 100))

def register_mcp_openapi(app: FastAPI):
    """
    Registers a custom endpoint to serve OpenAPI definitions tailored specifically for an MCP Gateway.
    
    This function modifies the default OpenAPI JSON payload dynamically, injecting detailed
    LLM prompt instructions into descriptions for the API endpoints.
    """
    @app.get("/api/mcp/openapi.json", include_in_schema=False)
    def get_mcp_openapi():
        # 1. Generate the standard schema
        schema = get_openapi(
            title="Todo List API (MCP Gateway Version)",
            version="1.0.0",
            description="Exposes customized tool descriptions for LLM agents utilizing MCP.",
            routes=app.routes,
        )
        
        # 2. Programmatically inject custom instructions for the LLM
        paths = schema.get("paths", {})
        
        # Override for POST /api/todos (Create Todo)
        if "/api/todos" in paths and "post" in paths["/api/todos"]:
            paths["/api/todos"]["post"]["description"] = (
                "TOOL INSTRUCTION: Use this tool to create a new Todo item in the user's list. "
                "CRITICAL: The 'title' argument is mandatory and must contain a non-empty string. "
                "SECURITY: You MUST supply a valid 'Authorization: Bearer admin-token' header to call this tool successfully."
            )
            
        # Override for PUT /api/todos/{todo_id}/toggle (Toggle Todo)
        if "/api/todos/{todo_id}/toggle" in paths and "put" in paths["/api/todos/{todo_id}/toggle"]:
            paths["/api/todos/{todo_id}/toggle"]["put"]["description"] = (
                "TOOL INSTRUCTION: Use this tool to invert the completion status of an existing Todo item. "
                "CRITICAL: You must supply the integer 'todo_id'. "
                "SECURITY: You MUST supply a valid 'Authorization: Bearer admin-token' header."
            )
            
        # Override for DELETE /api/todos/{todo_id} (Delete Todo)
        if "/api/todos/{todo_id}" in paths and "delete" in paths["/api/todos/{todo_id}"]:
            paths["/api/todos/{todo_id}"]["delete"]["description"] = (
                "TOOL INSTRUCTION: Use this tool to permanently remove a Todo item from the database. "
                "CRITICAL: You must supply the integer 'todo_id'. "
                "SECURITY: You MUST supply a valid 'Authorization: Bearer admin-token' header."
            )

        return schema
