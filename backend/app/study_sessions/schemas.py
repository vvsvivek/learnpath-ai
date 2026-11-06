"""
Study Session Pydantic v2 Schemas.
"""

from datetime import datetime
from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class StartSession(BaseModel):
    """Payload to initiate a study session timer."""
    model_config = ConfigDict(str_strip_whitespace=True)

    course_id: str | None = None
    notes: str | None = Field(default=None, max_length=500)


class EndSession(BaseModel):
    """Payload to terminate a study session."""
    model_config = ConfigDict(str_strip_whitespace=True)

    notes: str | None = Field(default=None, max_length=1000)
    duration_minutes: int | None = Field(default=None, ge=1, le=1440)


class StudySession(BaseModel):
    """Recorded study session entity."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    user_id: str
    course_id: str | None = None
    start_time: datetime | str
    end_time: datetime | str | None = None
    duration_minutes: int = 0
    notes: str | None = None
    course: dict[str, Any] | None = None
