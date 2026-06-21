from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

# HTTPBearer automatically checks the "Authorization" header for "Bearer <token>"
security = HTTPBearer()

def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> dict:
    """
    Extracts the Bearer token and performs authentication (identity verification).
    
    This function acts similarly to Spring Security's TokenAuthenticationFilter.java.
    It inspects the incoming token and maps it to a user identity and role.
    
    Mock Tokens:
      - "admin-token" -> {"username": "admin_user", "role": "ADMIN"}
      - "user-token"  -> {"username": "normal_user", "role": "USER"}
    """
    token = credentials.credentials
    if token == "admin-token":
        return {"username": "admin_user", "role": "ADMIN"}
    elif token == "user-token":
        return {"username": "normal_user", "role": "USER"}
    else:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired Bearer Token",
            headers={"WWW-Authenticate": "Bearer"},
        )

def verify_role(required_roles: list[str]):
    """
    Checks if the authenticated user has one of the allowed roles.
    
    This function behaves similarly to Spring Security's Method Security
    annotations like `@PreAuthorize("hasRole('ADMIN')")`.
    """
    def dependency(current_user: dict = Depends(get_current_user)):
        if current_user["role"] not in required_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Permission denied: Insufficient privileges"
            )
        return current_user
    return dependency
