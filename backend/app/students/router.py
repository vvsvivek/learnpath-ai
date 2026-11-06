"""
Student API Router for Profiles, Dashboards, and Learning Statistics.
"""

from typing import Any
from fastapi import APIRouter, Depends, status
from supabase import AsyncClient
from app.core.dependencies import get_supabase, require_student
from app.students.schemas import (
    StudentDashboard,
    StudentProfile,
    StudyStats,
    UpdateProfileSchema,
)
from app.students.service import (
    get_student_dashboard,
    get_student_profile,
    get_study_stats,
    update_student_profile,
)

router = APIRouter(prefix="/students", tags=["Students"])


@router.get(
    "/dashboard",
    response_model=StudentDashboard,
    status_code=status.HTTP_200_OK,
    summary="Get aggregated student dashboard overview",
)
async def get_dashboard_endpoint(
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> StudentDashboard:
    """Fetch complete student dashboard with study statistics, enrolled courses, and weak topics."""
    user_id = str(current_user["id"])
    return await get_student_dashboard(user_id=user_id, supabase=supabase)


@router.get(
    "/profile",
    response_model=StudentProfile,
    status_code=status.HTTP_200_OK,
    summary="Retrieve student profile",
)
async def get_profile_endpoint(
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> StudentProfile:
    """Get student academic details, branch, semester, CGPA, and interests."""
    user_id = str(current_user["id"])
    return await get_student_profile(user_id=user_id, supabase=supabase)


@router.put(
    "/profile",
    response_model=StudentProfile,
    status_code=status.HTTP_200_OK,
    summary="Update student profile",
)
async def update_profile_endpoint(
    payload: UpdateProfileSchema,
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> StudentProfile:
    """Modify student academic info, semester, CGPA, or learning interests."""
    user_id = str(current_user["id"])
    return await update_student_profile(user_id=user_id, data=payload, supabase=supabase)


@router.get(
    "/stats",
    response_model=StudyStats,
    status_code=status.HTTP_200_OK,
    summary="Retrieve aggregate study statistics",
)
async def get_stats_endpoint(
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> StudyStats:
    """Calculate and return total study time, completion rates, and average quiz scores."""
    user_id = str(current_user["id"])
    return await get_study_stats(user_id=user_id, supabase=supabase)
