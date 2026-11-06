"""
Course, Recommendation, and Assignment Pydantic v2 Schemas.
"""

from datetime import datetime
from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class Course(BaseModel):
    """Course entity schema."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    title: str
    description: str | None = None
    category: str | None = None
    difficulty: str = "Beginner"
    is_custom: bool = False
    created_by: str | None = None
    created_at: datetime | str | None = None


class CourseRecommendation(BaseModel):
    """AI-generated course recommendation."""
    model_config = ConfigDict(from_attributes=True)

    id: str | None = None
    course_title: str
    description: str | None = None
    category: str | None = None
    difficulty: str = "Beginner"
    reason: str
    match_score: float = 0.90
    is_dismissed: bool = False


class CourseEnrollment(BaseModel):
    """Course enrollment record schema."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    user_id: str
    course_id: str
    status: str = "enrolled"
    progress_percentage: float = 0.0
    enrolled_at: datetime | str | None = None
    completed_at: datetime | str | None = None
    course: dict[str, Any] | None = None


class CreateCustomCourse(BaseModel):
    """Schema for student or faculty creating a customized learning course."""
    model_config = ConfigDict(str_strip_whitespace=True)

    title: str = Field(..., min_length=3, max_length=200)
    description: str | None = Field(default=None, max_length=1000)
    category: str | None = Field(default="Computer Science", max_length=100)
    difficulty: str = Field(default="Intermediate", max_length=50)


class AssignCourseRequest(BaseModel):
    """Faculty request schema to assign a course to a student cohort."""
    model_config = ConfigDict(str_strip_whitespace=True)

    course_id: str
    academic_year: int = Field(..., ge=1, le=5)
    branch: str = Field(..., min_length=2, max_length=100)
    semester: int = Field(..., ge=1, le=10)
    due_date: datetime | str | None = None


class FacultyCourseAssignment(BaseModel):
    """Faculty course assignment schema."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    faculty_id: str
    course_id: str
    academic_year: int
    branch: str
    semester: int
    due_date: datetime | str | None = None
    assigned_at: datetime | str | None = None
    course: dict[str, Any] | None = None


class AssignmentStats(BaseModel):
    """Analytics for a specific faculty course assignment."""
    assignment_id: str
    course_title: str
    target_group: str
    total_assigned_students: int = 0
    total_enrolled: int = 0
    total_completed: int = 0
    average_progress: float = 0.0
