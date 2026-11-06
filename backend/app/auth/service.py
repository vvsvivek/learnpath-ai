"""
Authentication Business Logic and Database Operations.
"""

from datetime import datetime, timedelta, timezone
from typing import Any
import jwt
from fastapi import HTTPException, status
from supabase import AsyncClient
from app.auth.schemas import (
    MessageResponse,
    OnboardingSchema,
    OTPRequestSchema,
    OTPVerifySchema,
    TokenResponse,
)
from app.core.config import settings
from app.core.email import send_otp_email
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    generate_otp,
    hash_otp,
    verify_faculty_email,
    verify_otp as verify_otp_hash,
)
from app.database.models import (
    FACULTY_PROFILES_TABLE,
    OTP_RECORDS_TABLE,
    STUDENT_PROFILES_TABLE,
    USERS_TABLE,
)


async def request_otp(data: OTPRequestSchema, supabase: AsyncClient) -> MessageResponse:
    """Validate email eligibility, generate, hash, and persist OTP, and dispatch email.

    Args:
        data: OTPRequestSchema containing email and role.
        supabase: Active Supabase AsyncClient.

    Returns:
        MessageResponse with confirmation.

    Raises:
        HTTPException: If faculty email is unauthorized or storage fails.
    """
    clean_email = data.email.strip().lower()

    # Enforce faculty domain restrictions
    if data.role == "faculty":
        if not verify_faculty_email(clean_email):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=(
                    f"Faculty registration is restricted to Siddhartha Academy domains "
                    f"({', '.join(settings.FACULTY_ALLOWED_DOMAINS)}) and designated administrator accounts."
                ),
            )

    # Invalidate previous unused OTP records for this email
    try:
        await (
            supabase.table(OTP_RECORDS_TABLE)
            .update({"is_used": True})
            .eq("email", clean_email)
            .eq("role", data.role)
            .eq("is_used", False)
            .execute()
        )
    except Exception:
        pass  # Proceed even if update has no matching rows

    # Generate 6-digit OTP and secure bcrypt hash
    raw_otp = generate_otp()
    hashed = hash_otp(raw_otp)
    expires_at = (datetime.now(timezone.utc) + timedelta(minutes=settings.OTP_EXPIRE_MINUTES)).isoformat()

    # Store in Supabase
    try:
        insert_payload = {
            "email": clean_email,
            "otp_hash": hashed,
            "role": data.role,
            "purpose": "login",
            "attempts": 0,
            "is_used": False,
            "expires_at": expires_at,
        }
        await supabase.table(OTP_RECORDS_TABLE).insert(insert_payload).execute()
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to record authentication token: {str(exc)}",
        )

    # Dispatch email
    purpose = f"{data.role.title()} Authentication"
    email_dispatched = await send_otp_email(to_email=clean_email, otp=raw_otp, purpose=purpose)
    if not email_dispatched and settings.ENVIRONMENT == "production":
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Failed to dispatch verification email. Please verify your email configuration.",
        )

    return MessageResponse(
        message=f"A 6-digit verification code has been dispatched to {clean_email}."
    )


async def verify_otp(data: OTPVerifySchema, supabase: AsyncClient) -> TokenResponse:
    """Verify OTP, ensure/provision user record, and generate JWT tokens.

    Args:
        data: OTPVerifySchema with email, otp, and role.
        supabase: Active Supabase AsyncClient.

    Returns:
        TokenResponse with access token, refresh token, and user metadata.

    Raises:
        HTTPException: On invalid, expired, or non-existent OTPs.
    """
    clean_email = data.email.strip().lower()

    # Query latest active OTP record
    res = (
        await supabase.table(OTP_RECORDS_TABLE)
        .select("*")
        .eq("email", clean_email)
        .eq("role", data.role)
        .eq("is_used", False)
        .order("created_at", desc=True)
        .limit(1)
        .execute()
    )

    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No active verification code found for this account. Please request a new one.",
        )

    record = res.data[0]
    record_id = record["id"]
    attempts = record.get("attempts", 0)

    if attempts >= 5:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Too many incorrect attempts. Please request a fresh code.",
        )

    # Check expiry
    raw_exp = record["expires_at"]
    if isinstance(raw_exp, str):
        expires_at = datetime.fromisoformat(raw_exp.replace("Z", "+00:00"))
    else:
        expires_at = raw_exp
    
    if datetime.now(timezone.utc) > expires_at:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Verification code has expired. Please request a new one.",
        )

    # Verify bcrypt hash
    if not verify_otp_hash(data.otp, record["otp_hash"]):
        await (
            supabase.table(OTP_RECORDS_TABLE)
            .update({"attempts": attempts + 1})
            .eq("id", record_id)
            .execute()
        )
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid verification code. Attempts remaining: {4 - attempts}.",
        )

    # Mark OTP as successfully redeemed
    await (
        supabase.table(OTP_RECORDS_TABLE)
        .update({"is_used": True})
        .eq("id", record_id)
        .execute()
    )

    # Fetch or provision User
    user_query = await supabase.table(USERS_TABLE).select("*").eq("email", clean_email).limit(1).execute()
    if user_query.data:
        user = user_query.data[0]
        user_id = user["id"]
    else:
        new_user = await (
            supabase.table(USERS_TABLE)
            .insert({
                "email": clean_email,
                "role": data.role,
                "is_active": True,
            })
            .execute()
        )
        user = new_user.data[0]
        user_id = user["id"]

    # Check if student/faculty profile is completed
    onboarding_completed = False
    if data.role == "student":
        profile_res = await supabase.table(STUDENT_PROFILES_TABLE).select("id").eq("user_id", user_id).limit(1).execute()
        onboarding_completed = bool(profile_res.data)
    else:
        faculty_profile_res = await supabase.table(FACULTY_PROFILES_TABLE).select("id").eq("user_id", user_id).limit(1).execute()
        onboarding_completed = bool(faculty_profile_res.data)

    # Generate access and refresh tokens
    token_payload = {
        "sub": str(user_id),
        "email": clean_email,
        "role": data.role,
    }
    access_token = create_access_token(token_payload)
    refresh_token = create_refresh_token(token_payload)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        user=user,
        onboarding_completed=onboarding_completed,
    )


async def refresh_user_token(refresh_token: str, supabase: AsyncClient) -> dict[str, Any]:
    """Validate a refresh token and return a renewed access token.

    Args:
        refresh_token: Signed JWT refresh token.
        supabase: Active Supabase AsyncClient.

    Returns:
        Dict containing the fresh access token.

    Raises:
        HTTPException: On invalid or expired refresh token.
    """
    try:
        payload = decode_token(refresh_token)
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired refresh token.",
        )

    if payload.get("type") != "refresh":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token provided is not a valid refresh token.",
        )

    user_id = payload.get("sub")
    user_res = await supabase.table(USERS_TABLE).select("*").eq("id", user_id).limit(1).execute()
    if not user_res.data or not user_res.data[0].get("is_active", True):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Account associated with refresh token is invalid or inactive.",
        )

    user = user_res.data[0]
    token_payload = {
        "sub": str(user["id"]),
        "email": user["email"],
        "role": user["role"],
    }
    new_access_token = create_access_token(token_payload)
    return {
        "access_token": new_access_token,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
    }


async def complete_onboarding(
    user_id: str,
    data: OnboardingSchema,
    supabase: AsyncClient,
) -> dict[str, Any]:
    """Create or update a student profile upon completing onboarding.

    Args:
        user_id: UUID string of the authenticated student.
        data: OnboardingSchema details.
        supabase: Active Supabase AsyncClient.

    Returns:
        Created or updated student profile record.
    """
    profile_data = {
        "user_id": user_id,
        "full_name": data.full_name,
        "branch": data.branch,
        "academic_year": data.academic_year,
        "semester": data.semester,
        "cgpa": data.cgpa,
        "learning_interests": data.learning_interests,
        "updated_at": datetime.now(timezone.utc).isoformat(),
    }

    # Check if profile already exists
    existing = (
        await supabase.table(STUDENT_PROFILES_TABLE)
        .select("id")
        .eq("user_id", user_id)
        .limit(1)
        .execute()
    )

    if existing.data:
        res = (
            await supabase.table(STUDENT_PROFILES_TABLE)
            .update(profile_data)
            .eq("user_id", user_id)
            .execute()
        )
    else:
        profile_data["created_at"] = datetime.now(timezone.utc).isoformat()
        res = await supabase.table(STUDENT_PROFILES_TABLE).insert(profile_data).execute()

    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to record student onboarding profile.",
        )

    return res.data[0]
