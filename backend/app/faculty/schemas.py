"""
Faculty Schemas for Institutional Analytics and Student Monitoring.
"""

from typing import Any, Literal
from pydantic import BaseModel, ConfigDict, Field


class StudentFilter(BaseModel):
    """Filters for querying students by department, cohort, and domain."""
    model_config = ConfigDict(str_strip_whitespace=True)

    academic_year: int | None = Field(default=None, ge=1, le=5)
    branch: str | None = None
    semester: int | None = Field(default=None, ge=1, le=10)
    institutional_only: bool = Field(
        default=True,
        description="Filter exclusively for students with institutional domain (e.g. @vrsec.ac.in)",
    )


class StudentAnalytics(BaseModel):
    """Summarized learning analytics for an individual student."""
    model_config = ConfigDict(from_attributes=True)

    student_id: str
    user_id: str
    full_name: str
    email: str
    branch: str
    academic_year: int
    semester: int
    cgpa: float | None = None
    courses_enrolled: int = 0
    avg_quiz_score: float = 0.0
    study_hours: float = 0.0
    performance_tier: Literal["high_performing", "average", "needs_support"] = "average"


class StudentCategory(BaseModel):
    """Categorized segment of students (e.g. High Performing or Needs Support)."""
    category_name: str
    student_count: int
    students: list[StudentAnalytics] = Field(default_factory=list)


class FacultyDashboard(BaseModel):
    """Comprehensive institutional dashboard for faculty."""
    total_students: int = 0
    institutional_students_count: int = 0
    active_courses: int = 0
    average_class_performance: float = 0.0
    high_performing_count: int = 0
    needs_support_count: int = 0
    students_list: list[StudentAnalytics] = Field(default_factory=list)
    recent_assignments: list[dict[str, Any]] = Field(default_factory=list)
