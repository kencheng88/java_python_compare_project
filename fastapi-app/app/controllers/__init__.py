# Package-level exports for controllers.
# This simplifies imports in main.py by allowing us to import all routers directly from app.controllers.

from .web_controller import router as web_router
from .api_controller import router as api_router
from .debug_controller import router as debug_router, register_mcp_openapi
