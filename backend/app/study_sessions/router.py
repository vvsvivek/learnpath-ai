"""
Study Sessions API Router for Focus Timers and Time Tracking.
"""

from typing import Any
from fastapi import APIRouter, Depends, Query, status
from supabase import AsyncClient
from app.core.dependencies import get_supabase, require_student
from app.study_sessions.schemas import EndSession, StartSession, StudySession
from app.study_sessions.service import (
    end_study_session,
    get_total_study_time,
    get_user_study_sessions,
    start_study_session,
)

router = APIRouter(prefix="/study-sessions", tags=["Study Sessions"])


@router.post(
    "/start",
    response_model=StudySession,
    status_code=status.HTTP_201_CREATED,
    summary="Start a live study session timer",
)
async def start_session_endpoint(
    payload: StartSession,
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> StudySession:
    """Record session start time for active learning and focus tracking."""
    user_id = str(current_user["id"])
    return await start_study_session(user_id=user_id, data=payload, supabase=supabase)


@router.post(
    "/{id}/end",
    response_model=StudySession,
    status_code=status.HTTP_200_OK,
    summary="Stop and record a study session",
)
async def end_session_endpoint(
    id: str,
    payload: EndSession,
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> StudySession:
    """End the study session and compute duration minutes studied."""
    user_id = str(current_user["id"])
    return await end_study_session(session_id=id, user_id=user_id, data=payload, supabase=supabase)


@router.get(
    "",
    response_model=list[StudySession],
    status_code=status.HTTP_200_OK,
    summary="Get history of recorded study sessions",
)
async def list_sessions_endpoint(
    limit: int = Query(default=50, ge=1, le=200),
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> list[StudySession]:
    """Retrieve chronologically ordered list of past focus study sessions."""
    user_id = str(current_user["id"])
    return await get_user_study_sessions(user_id=user_id, supabase=supabase, limit=limit)


@router.get(
    "/total",
    response_model=dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Get total accumulated study time",
)
async def get_total_study_time_endpoint(
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """Compute aggregate hours and minutes studied."""
    user_id = str(current_user["id"])
    return await get_total_study_time(user_id=user_id, supabase=supabase)
