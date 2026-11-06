"""
Study Session Tracking and Timer Service.
"""

from datetime import datetime, timezone
from typing import Any
from fastapi import HTTPException, status
from supabase import AsyncClient
from app.database.models import STUDY_SESSIONS_TABLE
from app.study_sessions.schemas import EndSession, StartSession, StudySession


async def start_study_session(
    user_id: str,
    data: StartSession,
    supabase: AsyncClient,
) -> StudySession:
    """Initiate a live study session timer for a course.

    Args:
        user_id: UUID of student.
        data: StartSession payload.
        supabase: Active Supabase AsyncClient.

    Returns:
        Created StudySession model.
    """
    now_iso = datetime.now(timezone.utc).isoformat()
    session_payload = {
        "user_id": user_id,
        "course_id": data.course_id,
        "start_time": now_iso,
        "duration_minutes": 0,
        "notes": data.notes,
    }
    res = await supabase.table(STUDY_SESSIONS_TABLE).insert(session_payload).execute()
    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to initiate study session.",
        )
    return StudySession.model_validate(res.data[0])


async def end_study_session(
    session_id: str,
    user_id: str,
    data: EndSession,
    supabase: AsyncClient,
) -> StudySession:
    """Complete an ongoing study session and calculate total minutes studied.

    Args:
        session_id: UUID of study session.
        user_id: UUID of student.
        data: EndSession payload with optional notes or manual duration.
        supabase: Active Supabase AsyncClient.

    Returns:
        Updated StudySession model.
    """
    # Verify session exists
    session_res = (
        await supabase.table(STUDY_SESSIONS_TABLE)
        .select("*")
        .eq("id", session_id)
        .eq("user_id", user_id)
        .limit(1)
        .execute()
    )
    if not session_res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Study session not found.",
        )

    session = session_res.data[0]
    now = datetime.now(timezone.utc)
    now_iso = now.isoformat()

    # Calculate duration
    if data.duration_minutes is not None:
        duration_mins = data.duration_minutes
    else:
        raw_start = session["start_time"]
        if isinstance(raw_start, str):
            start_dt = datetime.fromisoformat(raw_start.replace("Z", "+00:00"))
        else:
            start_dt = raw_start
        delta_seconds = max(0, (now - start_dt).total_seconds())
        duration_mins = max(1, int(delta_seconds / 60))

    update_payload: dict[str, Any] = {
        "end_time": now_iso,
        "duration_minutes": duration_mins,
    }
    if data.notes:
        existing_notes = session.get("notes") or ""
        update_payload["notes"] = f"{existing_notes}\n{data.notes}".strip()

    res = (
        await supabase.table(STUDY_SESSIONS_TABLE)
        .update(update_payload)
        .eq("id", session_id)
        .execute()
    )
    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to update study session.",
        )
    return StudySession.model_validate(res.data[0])


async def get_user_study_sessions(
    user_id: str,
    supabase: AsyncClient,
    limit: int = 50,
) -> list[StudySession]:
    """Retrieve history of study sessions for the authenticated student.

    Args:
        user_id: UUID of student.
        supabase: Active Supabase AsyncClient.
        limit: Max sessions to retrieve.

    Returns:
        List of StudySession models.
    """
    res = (
        await supabase.table(STUDY_SESSIONS_TABLE)
        .select("*, courses(title)")
        .eq("user_id", user_id)
        .order("start_time", desc=True)
        .limit(limit)
        .execute()
    )
    return [StudySession.model_validate(row) for row in (res.data or [])]


async def get_total_study_time(
    user_id: str,
    supabase: AsyncClient,
) -> dict[str, Any]:
    """Calculate aggregate total study time and session counts.

    Args:
        user_id: UUID of student.
        supabase: Active Supabase AsyncClient.

    Returns:
        Dict with total_minutes, total_hours, and session_count.
    """
    res = (
        await supabase.table(STUDY_SESSIONS_TABLE)
        .select("duration_minutes")
        .eq("user_id", user_id)
        .execute()
    )
    sessions = res.data or []
    total_minutes = sum(s.get("duration_minutes") or 0 for s in sessions)

    return {
        "total_minutes": total_minutes,
        "total_hours": round(total_minutes / 60.0, 1),
        "session_count": len(sessions),
    }
