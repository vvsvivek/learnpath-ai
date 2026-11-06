"""
Application Configuration Module.

Loads and validates environment variables using Pydantic Settings v2.
"""

import json
from typing import Any
from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings and environment configuration."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )

    # Supabase Configuration
    SUPABASE_URL: str = ""
    SUPABASE_ANON_KEY: str = ""
    SUPABASE_SERVICE_ROLE_KEY: str = ""

    # Brevo Configuration
    BREVO_API_KEY: str = ""
    BREVO_SENDER_EMAIL: str = "noreply@siddhartha.ac.in"
    BREVO_SENDER_NAME: str = "AI Learning Path Generator"

    # Gemini Configuration
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-2.5-flash"

    # Security & JWT Configuration
    JWT_SECRET_KEY: str = "super-secret-jwt-key-minimum-32-characters-required-for-production"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 7
    OTP_EXPIRE_MINUTES: int = 5

    # Domain & Access Control Configuration
    FACULTY_ALLOWED_DOMAINS: list[str] = ["@siddhartha.ac.in"]
    FACULTY_SPECIAL_EMAILS: list[str] = ["vivekvaranasi99@gmail.com"]
    STUDENT_INSTITUTIONAL_DOMAIN: str = "@vrsec.ac.in"

    # Application Configuration
    CORS_ORIGINS: list[str] = ["*"]
    ENVIRONMENT: str = "development"
    PORT: int = 8000

    @field_validator(
        "FACULTY_ALLOWED_DOMAINS",
        "FACULTY_SPECIAL_EMAILS",
        "CORS_ORIGINS",
        mode="before",
    )
    @classmethod
    def parse_list_fields(cls, v: Any) -> list[str]:
        """Parse list fields from JSON string or comma-separated strings if needed."""
        if isinstance(v, list):
            return [str(item).strip() for item in v]
        if isinstance(v, str):
            v_trimmed = v.strip()
            if v_trimmed.startswith("[") and v_trimmed.endswith("]"):
                try:
                    parsed = json.loads(v_trimmed)
                    if isinstance(parsed, list):
                        return [str(item).strip() for item in parsed]
                except Exception:
                    pass
            # Fallback to comma separated
            return [item.strip() for item in v_trimmed.split(",") if item.strip()]
        return []


settings = Settings()
