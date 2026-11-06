"""
Reporting API Router for Student Diagnostics and Faculty Institutional Insights.
"""

from typing import Any
from fastapi import APIRouter, Depends, Query, status
from supabase import AsyncClient
from app.core.dependencies import get_current_user, get_supabase, require_faculty
from app.faculty.schemas import StudentFilter
from app.reports.schemas import FacultyReport, StudentReport
from app.reports.service import get_faculty_report, get_student_report

router = APIRouter(prefix="/reports", tags=["Reports"])


@router.get(
    "/student",
    response_model=StudentReport,
    status_code=status.HTTP_200_OK,
    summary="Get detailed personalized student learning analytics and AI insights",
)
async def get_student_report_endpoint(
    student_id: str | None = Query(default=None, description="Optional student user_id for faculty viewing"),
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> StudentReport:
    """Retrieve full learning diagnostics, progress metrics, and AI recommendations."""
    # If faculty requested a specific student_id, allow it; otherwise default to self
    target_id = str(current_user["id"])
    if student_id and current_user.get("role") == "faculty":
        target_id = student_id

    return await get_student_report(user_id=target_id, supabase=supabase)


@router.get(
    "/faculty",
    response_model=FacultyReport,
    status_code=status.HTTP_200_OK,
    summary="Get aggregated departmental academic report (Faculty only)",
)
async def get_faculty_report_endpoint(
    academic_year: int | None = Query(default=None, ge=1, le=5),
    branch: str | None = Query(default=None),
    semester: int | None = Query(default=None, ge=1, le=10),
    institutional_only: bool = Query(default=True),
    current_user: dict[str, Any] = Depends(require_faculty),
    supabase: AsyncClient = Depends(get_supabase),
) -> FacultyReport:
    """Consolidated departmental cohort performance, metrics, and AI interventions."""
    filters = StudentFilter(
        academic_year=academic_year,
        branch=branch,
        semester=semester,
        institutional_only=institutional_only,
    )
    return await get_faculty_report(filters=filters, supabase=supabase)
