"""
Learning Paths and Progress Tracking API Router.
"""

from typing import Any
from fastapi import APIRouter, Depends, status
from supabase import AsyncClient
from app.core.dependencies import get_current_user, get_supabase
from app.learning_paths.schemas import (
    LearningPath,
    LearningPhase,
    PhaseProgress,
)
from app.learning_paths.service import (
    complete_topic,
    generate_learning_path,
    get_learning_path,
    get_phase_detail,
    get_phase_progress,
)

router = APIRouter(prefix="/learning-paths", tags=["Learning Paths"])


@router.post(
    "/{course_id}/generate",
    response_model=LearningPath,
    status_code=status.HTTP_201_CREATED,
    summary="Generate AI-powered 4-phase learning path for a course",
)
async def generate_path_endpoint(
    course_id: str,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> LearningPath:
    """Generate structured curriculum phases, core topics, and curated learning resources."""
    user_id = str(current_user["id"])
    return await generate_learning_path(course_id=course_id, user_id=user_id, supabase=supabase)


@router.get(
    "/{course_id}",
    response_model=LearningPath,
    status_code=status.HTTP_200_OK,
    summary="Get full learning path for course with completion status",
)
async def get_path_endpoint(
    course_id: str,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> LearningPath:
    """Retrieve all phases, topics, and completion status for the current student."""
    user_id = str(current_user["id"])
    return await get_learning_path(course_id=course_id, user_id=user_id, supabase=supabase)


@router.get(
    "/phases/{phase_id}",
    response_model=LearningPhase,
    status_code=status.HTTP_200_OK,
    summary="Get single phase details with topics and resources",
)
async def get_phase_endpoint(
    phase_id: str,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> LearningPhase:
    """Inspect topics, study resources, and completion marks for a specific phase."""
    user_id = str(current_user["id"])
    return await get_phase_detail(phase_id=phase_id, user_id=user_id, supabase=supabase)


@router.post(
    "/topics/{topic_id}/complete",
    response_model=dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Mark a topic as completed by the student",
)
async def complete_topic_endpoint(
    topic_id: str,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """Flag topic as complete and recalculate overall course completion percentage."""
    user_id = str(current_user["id"])
    return await complete_topic(user_id=user_id, topic_id=topic_id, supabase=supabase)


@router.get(
    "/phases/{phase_id}/progress",
    response_model=PhaseProgress,
    status_code=status.HTTP_200_OK,
    summary="Get progress percentage for a phase",
)
async def get_phase_progress_endpoint(
    phase_id: str,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> PhaseProgress:
    """Calculate topic completion count and percentage for a learning phase."""
    user_id = str(current_user["id"])
    return await get_phase_progress(user_id=user_id, phase_id=phase_id, supabase=supabase)
