from pydantic import BaseModel, ConfigDict
from datetime import datetime
from typing import Optional

class TodoCreate(BaseModel):
    """Schema for creating a Todo. Serves as a DTO (Data Transfer Object)."""
    title: str
    description: Optional[str] = None

class TodoResponse(BaseModel):
    """Schema for returning a Todo. Matches the DB Model fields."""
    model_config = ConfigDict(from_attributes=True) # Allows mapping SQLAlchemy objects to Pydantic

    id: int
    title: str
    description: Optional[str] = None
    completed: bool
    created_at: datetime
    updated_at: datetime

class TodoLogResponse(BaseModel):
    """Schema for returning an Audit Log."""
    model_config = ConfigDict(from_attributes=True)

    id: int
    todo_id: int
    action: str
    details: Optional[str] = None
    created_at: datetime
