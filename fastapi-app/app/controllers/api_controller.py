from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.services.todo_service import TodoService
from app.models.schemas import TodoCreate, TodoResponse, TodoLogResponse
from app.security import verify_role

router = APIRouter(prefix="/api/todos", tags=["Todo API"])
todo_service = TodoService()

@router.get(
    "",
    response_model=list[TodoResponse],
    summary="Get all Todo items",
    description="Accessible by roles: USER, ADMIN. Fetches a complete list of todos.",
    dependencies=[Depends(verify_role(["USER", "ADMIN"]))]
)
def get_all_todos(db: Session = Depends(get_db)):
    """
    Controller endpoint to fetch all todo items.
    
    This is equivalent to ApiController.getAllTodos() in Spring Boot.
    Secured using the verify_role dependency check.
    """
    return todo_service.get_all_todos(db)

@router.post(
    "",
    response_model=TodoResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new Todo item",
    description="Accessible by roles: ADMIN only. Saves a new Todo and creates an audit log entry in a transaction.",
    dependencies=[Depends(verify_role(["ADMIN"]))]
)
def create_todo(todo_in: TodoCreate, db: Session = Depends(get_db)):
    """
    Controller endpoint to create a new todo.
    
    This is equivalent to ApiController.createTodo() in Spring Boot.
    Demonstrates transaction control; any error will rollback both the Todo and the Log creation.
    """
    return todo_service.create_todo(db, todo_in.title, todo_in.description)

@router.put(
    "/{todo_id}/toggle",
    response_model=TodoResponse,
    summary="Toggle Todo completed status",
    description="Accessible by roles: ADMIN only. Inverts the completed status and logs it.",
    dependencies=[Depends(verify_role(["ADMIN"]))]
)
def toggle_todo(todo_id: int, db: Session = Depends(get_db)):
    """
    Controller endpoint to toggle a todo item.
    
    This is equivalent to ApiController.toggleTodo() in Spring Boot.
    """
    return todo_service.toggle_todo_status(db, todo_id)

@router.delete(
    "/{todo_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a Todo item",
    description="Accessible by roles: ADMIN only. Deletes the todo item and logs it.",
    dependencies=[Depends(verify_role(["ADMIN"]))]
)
def delete_todo(todo_id: int, db: Session = Depends(get_db)):
    """
    Controller endpoint to delete a todo item.
    
    This is equivalent to ApiController.deleteTodo() in Spring Boot.
    """
    todo_service.delete_todo(db, todo_id)
    return None

@router.get(
    "/logs",
    response_model=list[TodoLogResponse],
    summary="View Audit Logs",
    description="Accessible by roles: ADMIN only. Retrieves the recent database audit logs.",
    dependencies=[Depends(verify_role(["ADMIN"]))]
)
def get_audit_logs(db: Session = Depends(get_db)):
    """
    Controller endpoint to fetch database audit logs.
    
    This is equivalent to ApiController.getAuditLogs() in Spring Boot.
    Allows easy verification of transaction records.
    """
    return todo_service.get_recent_logs(db)
