"""
Course Management and Recommendations API Router.
"""

from typing import Any
from fastapi import APIRouter, Depends, status
from supabase import AsyncClient
from app.core.dependencies import get_current_user, get_supabase, require_faculty, require_student
from app.courses.schemas import (
    AssignCourseRequest,
    AssignmentStats,
    Course,
    CourseEnrollment,
    CourseRecommendation,
    CreateCustomCourse,
    FacultyCourseAssignment,
)
from app.courses.service import (
    assign_course_to_cohort,
    create_custom_course,
    dismiss_recommendation,
    enroll_in_course,
    get_assignment_stats,
    get_course_recommendations,
    get_enrolled_courses,
    get_faculty_assignments,
)

router = APIRouter(prefix="/courses", tags=["Courses"])


@router.get(
    "/recommendations",
    response_model=list[CourseRecommendation],
    status_code=status.HTTP_200_OK,
    summary="Get personalized course recommendations for student",
)
async def get_recommendations_endpoint(
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> list[CourseRecommendation]:
    """Fetch AI-generated recommendations based on student's branch, semester, and interests."""
    user_id = str(current_user["id"])
    return await get_course_recommendations(user_id=user_id, supabase=supabase)


@router.post(
    "/custom",
    response_model=Course,
    status_code=status.HTTP_201_CREATED,
    summary="Create a custom tailored course",
)
async def create_custom_course_endpoint(
    payload: CreateCustomCourse,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> Course:
    """Create a new custom course and auto-enroll the requesting user."""
    user_id = str(current_user["id"])
    return await create_custom_course(user_id=user_id, data=payload, supabase=supabase)


@router.post(
    "/{course_id}/enroll",
    response_model=CourseEnrollment,
    status_code=status.HTTP_200_OK,
    summary="Enroll student into a course",
)
async def enroll_course_endpoint(
    course_id: str,
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> CourseEnrollment:
    """Enroll the authenticated student into the designated course."""
    user_id = str(current_user["id"])
    return await enroll_in_course(user_id=user_id, course_id=course_id, supabase=supabase)


@router.get(
    "/enrolled",
    response_model=list[CourseEnrollment],
    status_code=status.HTTP_200_OK,
    summary="Get all courses enrolled by the student",
)
async def get_enrolled_courses_endpoint(
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> list[CourseEnrollment]:
    """Retrieve enrolled courses with current progress percentage."""
    user_id = str(current_user["id"])
    return await get_enrolled_courses(user_id=user_id, supabase=supabase)


@router.post(
    "/assign",
    response_model=FacultyCourseAssignment,
    status_code=status.HTTP_201_CREATED,
    summary="Assign course to an academic cohort (Faculty only)",
)
async def assign_course_endpoint(
    payload: AssignCourseRequest,
    current_user: dict[str, Any] = Depends(require_faculty),
    supabase: AsyncClient = Depends(get_supabase),
) -> FacultyCourseAssignment:
    """Faculty assigns a course to an entire branch, year, and semester cohort."""
    faculty_id = str(current_user["id"])
    return await assign_course_to_cohort(faculty_id=faculty_id, data=payload, supabase=supabase)


@router.get(
    "/assignments",
    response_model=list[FacultyCourseAssignment],
    status_code=status.HTTP_200_OK,
    summary="List course assignments created by faculty (Faculty only)",
)
async def get_assignments_endpoint(
    current_user: dict[str, Any] = Depends(require_faculty),
    supabase: AsyncClient = Depends(get_supabase),
) -> list[FacultyCourseAssignment]:
    """Retrieve all cohort course assignments initiated by the current faculty."""
    faculty_id = str(current_user["id"])
    return await get_faculty_assignments(faculty_id=faculty_id, supabase=supabase)


@router.get(
    "/assignments/{id}/stats",
    response_model=AssignmentStats,
    status_code=status.HTTP_200_OK,
    summary="Get cohort progress analytics for an assignment (Faculty only)",
)
async def get_assignment_stats_endpoint(
    id: str,
    current_user: dict[str, Any] = Depends(require_faculty),
    supabase: AsyncClient = Depends(get_supabase),
) -> AssignmentStats:
    """Inspect student enrollment, completion, and average progress on an assigned course."""
    return await get_assignment_stats(assignment_id=id, supabase=supabase)


@router.put(
    "/recommendations/{id}/dismiss",
    response_model=dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Dismiss an AI course recommendation",
)
async def dismiss_recommendation_endpoint(
    id: str,
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """Mark a recommendation as dismissed so it does not reappear."""
    user_id = str(current_user["id"])
    return await dismiss_recommendation(user_id=user_id, recommendation_id=id, supabase=supabase)
