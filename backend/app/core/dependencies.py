"""
FastAPI Dependencies for Authentication, Role Enforcement, and Database Access.
"""

from typing import Any
import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from supabase import AsyncClient
from app.core.security import decode_token
from app.database.connection import get_client

security_scheme = HTTPBearer(auto_error=True)


async def get_supabase() -> AsyncClient:
    """Dependency that returns the Supabase AsyncClient singleton."""
    return get_client()


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security_scheme),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """Validate JWT access token and return the authenticated user record.

    Args:
        credentials: Bearer token provided in Authorization header.
        supabase: Active Supabase AsyncClient.

    Returns:
        User record dict.

    Raises:
        HTTPException: If token is expired, invalid, or user does not exist/is inactive.
    """
    token = credentials.credentials
    try:
        payload = decode_token(token)
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication token has expired. Please log in again.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token_type = payload.get("type")
    if token_type != "access":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token type. Access token required.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token payload is missing subject identifier.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # Fetch user from Supabase
    try:
        response = await supabase.table("users").select("*").eq("id", user_id).limit(1).execute()
        if not response.data:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User account not found.",
                headers={"WWW-Authenticate": "Bearer"},
            )
        user: dict[str, Any] = response.data[0]
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Database query error while authenticating user: {str(exc)}",
        )

    if not user.get("is_active", True):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account has been deactivated.",
        )

    return user


async def require_student(
    current_user: dict[str, Any] = Depends(get_current_user),
) -> dict[str, Any]:
    """Dependency that ensures the authenticated user possesses the 'student' role."""
    if current_user.get("role") != "student":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access forbidden: this endpoint is exclusively for students.",
        )
    return current_user


async def require_faculty(
    current_user: dict[str, Any] = Depends(get_current_user),
) -> dict[str, Any]:
    """Dependency that ensures the authenticated user possesses the 'faculty' role."""
    if current_user.get("role") != "faculty":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Access forbidden: this endpoint is exclusively for faculty members.",
        )
    return current_user
