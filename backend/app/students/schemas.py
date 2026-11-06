"""
Student Schemas for Profile, Dashboard, and Academic Analytics.
"""

from datetime import datetime
from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class StudentProfile(BaseModel):
    """Student profile data model."""
    model_config = ConfigDict(from_attributes=True)

    id: str | None = None
    user_id: str
    full_name: str
    branch: str
    academic_year: int
    semester: int
    cgpa: float | None = None
    learning_interests: list[str] = Field(default_factory=list)
    created_at: datetime | str | None = None
    updated_at: datetime | str | None = None


class UpdateProfileSchema(BaseModel):
    """Schema for updating student profile."""
    model_config = ConfigDict(str_strip_whitespace=True)

    full_name: str | None = Field(default=None, min_length=2, max_length=150)
    branch: str | None = Field(default=None, min_length=2, max_length=100)
    academic_year: int | None = Field(default=None, ge=1, le=5)
    semester: int | None = Field(default=None, ge=1, le=10)
    cgpa: float | None = Field(default=None, ge=0.0, le=10.0)
    learning_interests: list[str] | None = None


class FocusTopic(BaseModel):
    """Identified focus/struggling topic requiring student review."""
    topic_id: str
    topic_title: str
    course_title: str | None = None
    phase_title: str | None = None
    mastery_level: str = "needs_practice"
    last_quiz_score: float | None = None


class StudyStats(BaseModel):
    """Aggregated study and engagement statistics."""
    total_study_minutes: int = 0
    total_study_hours: float = 0.0
    courses_enrolled: int = 0
    completed_courses: int = 0
    quizzes_taken: int = 0
    average_quiz_score: float = 0.0
    streak_days: int = 1


class StudentDashboard(BaseModel):
    """Consolidated student home dashboard payload."""
    profile: StudentProfile | None = None
    stats: StudyStats
    enrolled_courses: list[dict[str, Any]] = Field(default_factory=list)
    focus_topics: list[FocusTopic] = Field(default_factory=list)
    recent_quizzes: list[dict[str, Any]] = Field(default_factory=list)
