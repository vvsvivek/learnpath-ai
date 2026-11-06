"""
Faculty API Router for Dashboards, Institutional Analytics, and Student Breakdown.
"""

from typing import Any
from fastapi import APIRouter, Depends, Query, status
from supabase import AsyncClient
from app.core.dependencies import get_supabase, require_faculty
from app.faculty.schemas import (
    FacultyDashboard,
    StudentAnalytics,
    StudentCategory,
    StudentFilter,
)
from app.faculty.service import (
    categorize_students_list,
    get_faculty_dashboard,
    get_faculty_students,
    get_student_detail,
)

router = APIRouter(prefix="/faculty", tags=["Faculty"])


@router.get(
    "/dashboard",
    response_model=FacultyDashboard,
    status_code=status.HTTP_200_OK,
    summary="Get institutional faculty dashboard overview",
)
async def get_dashboard_endpoint(
    current_user: dict[str, Any] = Depends(require_faculty),
    supabase: AsyncClient = Depends(get_supabase),
) -> FacultyDashboard:
    """Fetch high-level department metrics, student distribution, and active assignments."""
    user_id = str(current_user["id"])
    return await get_faculty_dashboard(user_id=user_id, supabase=supabase)


@router.get(
    "/students",
    response_model=list[StudentAnalytics],
    status_code=status.HTTP_200_OK,
    summary="Filter and list students with performance metrics",
)
async def get_students_endpoint(
    academic_year: int | None = Query(default=None, ge=1, le=5),
    branch: str | None = Query(default=None),
    semester: int | None = Query(default=None, ge=1, le=10),
    institutional_only: bool = Query(default=True, description="Filter for institutional domain (e.g. @vrsec.ac.in)"),
    current_user: dict[str, Any] = Depends(require_faculty),
    supabase: AsyncClient = Depends(get_supabase),
) -> list[StudentAnalytics]:
    """Search and filter students by year, branch, semester, and institutional email affiliation."""
    filters = StudentFilter(
        academic_year=academic_year,
        branch=branch,
        semester=semester,
        institutional_only=institutional_only,
    )
    return await get_faculty_students(filters=filters, supabase=supabase)


@router.get(
    "/students/categories",
    response_model=list[StudentCategory],
    status_code=status.HTTP_200_OK,
    summary="Categorize students into performance segments",
)
async def get_student_categories_endpoint(
    academic_year: int | None = Query(default=None, ge=1, le=5),
    branch: str | None = Query(default=None),
    semester: int | None = Query(default=None, ge=1, le=10),
    institutional_only: bool = Query(default=True),
    current_user: dict[str, Any] = Depends(require_faculty),
    supabase: AsyncClient = Depends(get_supabase),
) -> list[StudentCategory]:
    """Segment students into High Performing, Needs Support, and Average categories."""
    filters = StudentFilter(
        academic_year=academic_year,
        branch=branch,
        semester=semester,
        institutional_only=institutional_only,
    )
    students = await get_faculty_students(filters=filters, supabase=supabase)
    return categorize_students_list(students)


@router.get(
    "/students/{student_id}",
    response_model=dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Get detailed progress report for an individual student",
)
async def get_student_detail_endpoint(
    student_id: str,
    current_user: dict[str, Any] = Depends(require_faculty),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """View comprehensive course enrollments, quiz history, and weak topics for a student."""
    return await get_student_detail(student_id=student_id, supabase=supabase)
