"""
Authentication Request and Response Pydantic v2 Schemas.
"""

from typing import Any, Literal
from pydantic import BaseModel, ConfigDict, EmailStr, Field


class OTPRequestSchema(BaseModel):
    """Schema for requesting a one-time passcode."""
    model_config = ConfigDict(str_strip_whitespace=True)

    email: EmailStr
    role: Literal["student", "faculty"] = Field(
        ..., description="Role claiming access: student or faculty"
    )


class OTPVerifySchema(BaseModel):
    """Schema for verifying OTP and issuing JWT tokens."""
    model_config = ConfigDict(str_strip_whitespace=True)

    email: EmailStr
    otp: str = Field(..., min_length=6, max_length=6, description="6-digit verification code")
    role: Literal["student", "faculty"]


class RefreshTokenSchema(BaseModel):
    """Schema for refreshing access token."""
    refresh_token: str = Field(..., description="Valid JWT refresh token")


class TokenResponse(BaseModel):
    """Authentication token response payload."""
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user: dict[str, Any]
    onboarding_completed: bool = False


class OnboardingSchema(BaseModel):
    """Student onboarding schema."""
    model_config = ConfigDict(str_strip_whitespace=True)

    full_name: str = Field(..., min_length=2, max_length=150)
    branch: str = Field(..., min_length=2, max_length=100)
    academic_year: int = Field(..., ge=1, le=5)
    semester: int = Field(..., ge=1, le=10)
    cgpa: float | None = Field(default=None, ge=0.0, le=10.0)
    learning_interests: list[str] = Field(default_factory=list)


class MessageResponse(BaseModel):
    """Standard message response."""
    message: str
    success: bool = True
