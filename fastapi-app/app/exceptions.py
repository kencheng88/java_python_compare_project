from fastapi import Request, status
from fastapi.responses import JSONResponse

class TodoTransactionError(Exception):
    """Custom exception raised when a database transaction fails or is rolled back."""
    def __init__(self, message: str):
        self.message = message

class TodoNotFoundError(Exception):
    """Custom exception raised when a requested Todo item is not found."""
    def __init__(self, message: str):
        self.message = message

def register_exception_handlers(app):
    """
    Registers global exception handlers on the FastAPI application instance.
    
    This matches the @RestControllerAdvice pattern in Spring Boot, allowing us
    to centralize error-to-JSON mappings rather than writing try-catch blocks
    inside individual controller endpoints.
    """
    
    @app.exception_handler(TodoTransactionError)
    async def todo_transaction_error_handler(request: Request, exc: TodoTransactionError):
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={
                "status": "error",
                "detail": exc.message,
                "message": exc.message,
                "type": "TodoTransactionError"
            }
        )

    @app.exception_handler(TodoNotFoundError)
    async def todo_not_found_error_handler(request: Request, exc: TodoNotFoundError):
        return JSONResponse(
            status_code=status.HTTP_404_NOT_FOUND,
            content={
                "status": "error",
                "detail": exc.message,
                "message": exc.message,
                "type": "TodoNotFoundError"
            }
        )
        
    @app.exception_handler(ValueError)
    async def value_error_handler(request: Request, exc: ValueError):
        """Fallback handler for validation errors in standard services."""
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={
                "status": "error",
                "detail": str(exc),
                "message": str(exc),
                "type": "ValueError"
            }
        )
