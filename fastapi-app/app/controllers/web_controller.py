import os
from fastapi import APIRouter, Request, Depends
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from app.database import get_db
from app.services.todo_service import TodoService

router = APIRouter()

# Locate the template directory under app/templates
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
templates = Jinja2Templates(directory=os.path.join(BASE_DIR, "templates"))

todo_service = TodoService()

@router.get("/", response_class=HTMLResponse, include_in_schema=False)
def get_index_page(request: Request, db: Session = Depends(get_db)):
    """
    Renders the Todo List management dashboard (Server-Side Rendering).
    
    This is equivalent to WebController.java in Spring Boot which returns
    a Thymeleaf view name. Here, we render the template directly.
    """
    todos = todo_service.get_all_todos(db)
    # Return the HTML template rendered with Jinja2 context variables
    return templates.TemplateResponse(
        "index.html",
        {"request": request, "todos": todos}
    )
