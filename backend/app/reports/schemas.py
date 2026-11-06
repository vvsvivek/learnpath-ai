"""
Reporting and Comprehensive Analytics Pydantic v2 Schemas.
"""

from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class CourseAnalytics(BaseModel):
    """Course level engagement and completion analytics."""
    course_id: str
    course_title: str
    total_enrolled: int = 0
    completion_rate: float = 0.0
    average_progress: float = 0.0


class QuizAnalytics(BaseModel):
    """Quiz performance analytics summary."""
    quiz_id: str
    quiz_title: str
    total_attempts: int = 0
    average_score: float = 0.0
    pass_rate: float = 0.0


class TopicAnalytics(BaseModel):
    """Topic mastery statistics."""
    topic_id: str
    topic_title: str
    mastery_percentage: float = 0.0
    total_completed_students: int = 0


class LearningAnalytics(BaseModel):
    """Key metric summary for student or faculty."""
    total_study_hours: float = 0.0
    topics_mastered: int = 0
    quiz_avg: float = 0.0
    active_days: int = 1


class StudentReport(BaseModel):
    """Comprehensive performance and progress report for an individual student."""
    student_id: str
    student_name: str
    branch: str
    academic_year: int
    overall_progress: float = 0.0
    total_study_hours: float = 0.0
    enrolled_courses: list[CourseAnalytics] = Field(default_factory=list)
    quiz_history: list[QuizAnalytics] = Field(default_factory=list)
    strong_topics: list[str] = Field(default_factory=list)
    weak_topics: list[str] = Field(default_factory=list)
    ai_insights: str = ""


class FacultyReport(BaseModel):
    """Institutional academic report for faculty overview."""
    department: str
    academic_year: int | None = None
    total_students: int = 0
    institutional_percentage: float = 0.0
    avg_cgpa: float = 0.0
    avg_quiz_score: float = 0.0
    courses_overview: list[CourseAnalytics] = Field(default_factory=list)
    performance_distribution: dict[str, int] = Field(default_factory=dict)
    key_recommendations: list[str] = Field(default_factory=list)
