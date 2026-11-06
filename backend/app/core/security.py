"""
Security, Cryptography, and Token Management Module.
"""

from datetime import datetime, timedelta, timezone
import secrets
from typing import Any
import bcrypt
import jwt
from app.core.config import settings


def generate_otp() -> str:
    """Generate a secure 6-digit numeric OTP."""
    return f"{secrets.randbelow(1_000_000):06d}"


def hash_otp(otp: str) -> str:
    """Hash an OTP string using bcrypt.
    
    Args:
        otp: The plain 6-digit OTP string.

    Returns:
        Hashed OTP string.
    """
    salt = bcrypt.gensalt(rounds=12)
    hashed = bcrypt.hashpw(otp.encode("utf-8"), salt)
    return hashed.decode("utf-8")


def verify_otp(otp: str, hashed_otp: str) -> bool:
    """Verify plain OTP against hashed OTP using bcrypt.

    Args:
        otp: The plain OTP string provided by user.
        hashed_otp: The bcrypt hashed OTP stored in database.

    Returns:
        True if valid, False otherwise.
    """
    try:
        return bcrypt.checkpw(otp.encode("utf-8"), hashed_otp.encode("utf-8"))
    except Exception:
        return False


def create_access_token(data: dict[str, Any], expires_delta: timedelta | None = None) -> str:
    """Create a signed JWT access token.

    Args:
        data: Payload dictionary to encode.
        expires_delta: Optional custom token expiration duration.

    Returns:
        Encoded JWT string.
    """
    to_encode = data.copy()
    now = datetime.now(timezone.utc)
    if expires_delta:
        expire = now + expires_delta
    else:
        expire = now + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode.update({
        "exp": expire,
        "iat": now,
        "type": "access",
    })
    return jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def create_refresh_token(data: dict[str, Any]) -> str:
    """Create a signed JWT refresh token.

    Args:
        data: Payload dictionary to encode.

    Returns:
        Encoded JWT string.
    """
    to_encode = data.copy()
    now = datetime.now(timezone.utc)
    expire = now + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    to_encode.update({
        "exp": expire,
        "iat": now,
        "type": "refresh",
    })
    return jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def decode_token(token: str) -> dict[str, Any]:
    """Decode and validate a JWT token.

    Args:
        token: The encoded JWT token string.

    Returns:
        Decoded payload dictionary.

    Raises:
        jwt.PyJWTError: When token is invalid, expired, or malformed.
    """
    return jwt.decode(
        token,
        settings.JWT_SECRET_KEY,
        algorithms=[settings.JWT_ALGORITHM],
    )


def verify_faculty_email(email: str) -> bool:
    """Check if an email address is authorized for faculty privileges.

    Faculty allowed domains: e.g. ["@siddhartha.ac.in"]
    Faculty special emails: e.g. ["vivekvaranasi99@gmail.com"]

    Args:
        email: Email address to check.

    Returns:
        True if authorized, False otherwise.
    """
    clean_email = email.strip().lower()
    
    # Check special override emails
    for special in settings.FACULTY_SPECIAL_EMAILS:
        if clean_email == special.strip().lower():
            return True

    # Check allowed institutional domains
    for domain in settings.FACULTY_ALLOWED_DOMAINS:
        clean_domain = domain.strip().lower()
        if not clean_domain.startswith("@"):
            clean_domain = f"@{clean_domain}"
        if clean_email.endswith(clean_domain):
            return True

    return False


def verify_student_email(email: str) -> bool:
    """Check if an email address is authorized for student privileges.

    Students are strictly restricted to @vrsec.ac.in institutional emails.
    """
    clean_email = email.strip().lower()
    student_domain = settings.STUDENT_INSTITUTIONAL_DOMAIN.strip().lower()
    if not student_domain.startswith("@"):
        student_domain = f"@{student_domain}"
    return clean_email.endswith(student_domain)


def get_role_for_email(email: str) -> str | None:
    """Determine and assign user role strictly and automatically based on email domain.

    Rules:
    - Faculty: @siddhartha.ac.in or authorized special emails (e.g. vivekvaranasi99@gmail.com)
    - Student: @vrsec.ac.in
    - All other emails: None (unauthorized, access denied)
    """
    clean_email = email.strip().lower()
    if verify_faculty_email(clean_email):
        return "faculty"
    if verify_student_email(clean_email):
        return "student"
    return None

