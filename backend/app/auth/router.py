"""
Authentication Router with Rate Limiting and Endpoints.
"""

from typing import Any
from fastapi import APIRouter, Depends, status
from supabase import AsyncClient
from app.auth.schemas import (
    MessageResponse,
    OnboardingSchema,
    OTPRequestSchema,
    OTPVerifySchema,
    RefreshTokenSchema,
    TokenResponse,
)
from app.auth.service import (
    complete_onboarding,
    refresh_user_token,
    request_otp,
    verify_otp,
)
from app.core.dependencies import get_current_user, get_supabase
from app.core.rate_limiter import rate_limit_dependency

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post(
    "/request-otp",
    response_model=MessageResponse,
    status_code=status.HTTP_200_OK,
    dependencies=[Depends(rate_limit_dependency(max_requests=5, window_seconds=300, key_prefix="otp_req"))],
    summary="Request a 6-digit one-time passcode for sign-in or registration",
)
async def handle_request_otp(
    payload: OTPRequestSchema,
    supabase: AsyncClient = Depends(get_supabase),
) -> MessageResponse:
    """Validate user email and dispatch a secure one-time passcode (OTP).

    - Faculty registrations must match Siddhartha Academy domains or authorized exceptions.
    - Students may register with any valid email.
    """
    return await request_otp(data=payload, supabase=supabase)


@router.post(
    "/verify-otp",
    response_model=TokenResponse,
    status_code=status.HTTP_200_OK,
    dependencies=[Depends(rate_limit_dependency(max_requests=10, window_seconds=300, key_prefix="otp_ver"))],
    summary="Verify OTP and receive JWT access and refresh tokens",
)
async def handle_verify_otp(
    payload: OTPVerifySchema,
    supabase: AsyncClient = Depends(get_supabase),
) -> TokenResponse:
    """Validate submitted OTP, provision user if new, and return signed JWT tokens."""
    return await verify_otp(data=payload, supabase=supabase)


@router.post(
    "/refresh",
    response_model=dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Renew access token using refresh token",
)
async def handle_refresh_token(
    payload: RefreshTokenSchema,
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """Provide a fresh access token given a valid unexpired refresh token."""
    return await refresh_user_token(refresh_token=payload.refresh_token, supabase=supabase)


@router.post(
    "/onboarding",
    response_model=dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Complete student profile onboarding",
)
async def handle_onboarding(
    payload: OnboardingSchema,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """Persist student academic background, branch, semester, and learning interests."""
    user_id = str(current_user["id"])
    return await complete_onboarding(user_id=user_id, data=payload, supabase=supabase)
