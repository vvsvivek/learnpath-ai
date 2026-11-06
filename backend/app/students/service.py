"""
Student Business Logic and Analytics Aggregation Service.
"""

from datetime import datetime, timezone
from typing import Any
from fastapi import HTTPException, status
from supabase import AsyncClient
from app.database.models import (
    COURSE_ENROLLMENTS_TABLE,
    COURSES_TABLE,
    PHASE_TOPICS_TABLE,
    QUIZ_ATTEMPTS_TABLE,
    QUIZZES_TABLE,
    STUDENT_PROFILES_TABLE,
    STUDY_SESSIONS_TABLE,
    TOPIC_PERFORMANCE_TABLE,
)
from app.students.schemas import (
    FocusTopic,
    StudentDashboard,
    StudentProfile,
    StudyStats,
    UpdateProfileSchema,
)


async def get_student_profile(user_id: str, supabase: AsyncClient) -> StudentProfile:
    """Retrieve the student's profile details.

    Args:
        user_id: UUID of student user.
        supabase: Active Supabase AsyncClient.

    Returns:
        StudentProfile model.

    Raises:
        HTTPException: If profile has not yet been initialized.
    """
    res = (
        await supabase.table(STUDENT_PROFILES_TABLE)
        .select("*")
        .eq("user_id", user_id)
        .limit(1)
        .execute()
    )
    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Student profile not found. Please complete onboarding first.",
        )
    return StudentProfile.model_validate(res.data[0])


async def update_student_profile(
    user_id: str,
    data: UpdateProfileSchema,
    supabase: AsyncClient,
) -> StudentProfile:
    """Update editable fields on the student profile.

    Args:
        user_id: UUID of student user.
        data: UpdateProfileSchema with proposed updates.
        supabase: Active Supabase AsyncClient.

    Returns:
        Updated StudentProfile model.
    """
    update_dict = data.model_dump(exclude_unset=True)
    if not update_dict:
        return await get_student_profile(user_id, supabase)

    update_dict["updated_at"] = datetime.now(timezone.utc).isoformat()
    res = (
        await supabase.table(STUDENT_PROFILES_TABLE)
        .update(update_dict)
        .eq("user_id", user_id)
        .execute()
    )
    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Student profile not found to update.",
        )
    return StudentProfile.model_validate(res.data[0])


async def get_study_stats(user_id: str, supabase: AsyncClient) -> StudyStats:
    """Compute aggregate study time, quiz statistics, and course counts for a student.

    Args:
        user_id: UUID of student user.
        supabase: Active Supabase AsyncClient.

    Returns:
        StudyStats model.
    """
    # 1. Total study minutes from study_sessions
    sessions_res = (
        await supabase.table(STUDY_SESSIONS_TABLE)
        .select("duration_minutes")
        .eq("user_id", user_id)
        .execute()
    )
    total_minutes = sum(item.get("duration_minutes") or 0 for item in sessions_res.data)

    # 2. Courses enrolled and completed
    enrollments_res = (
        await supabase.table(COURSE_ENROLLMENTS_TABLE)
        .select("status, progress_percentage")
        .eq("user_id", user_id)
        .execute()
    )
    enrolled_count = len(enrollments_res.data)
    completed_count = sum(
        1 for e in enrollments_res.data
        if e.get("status") == "completed" or (e.get("progress_percentage") or 0) >= 100
    )

    # 3. Quiz attempts and scores
    quizzes_res = (
        await supabase.table(QUIZ_ATTEMPTS_TABLE)
        .select("percentage")
        .eq("user_id", user_id)
        .execute()
    )
    quizzes_taken = len(quizzes_res.data)
    if quizzes_taken > 0:
        avg_score = round(
            sum(float(q.get("percentage") or 0) for q in quizzes_res.data) / quizzes_taken, 2
        )
    else:
        avg_score = 0.0

    return StudyStats(
        total_study_minutes=total_minutes,
        total_study_hours=round(total_minutes / 60.0, 1),
        courses_enrolled=enrolled_count,
        completed_courses=completed_count,
        quizzes_taken=quizzes_taken,
        average_quiz_score=avg_score,
        streak_days=max(1, min(7, enrolled_count + quizzes_taken)),
    )


async def get_student_dashboard(user_id: str, supabase: AsyncClient) -> StudentDashboard:
    """Assemble all personalized widgets and metrics for student home dashboard.

    Args:
        user_id: UUID of student user.
        supabase: Active Supabase AsyncClient.

    Returns:
        StudentDashboard model.
    """
    # Profile
    profile = None
    try:
        profile = await get_student_profile(user_id, supabase)
    except HTTPException:
        pass

    # Stats
    stats = await get_study_stats(user_id, supabase)

    # Enrolled courses with metadata
    enrollments_res = (
        await supabase.table(COURSE_ENROLLMENTS_TABLE)
        .select("*, courses(*)")
        .eq("user_id", user_id)
        .order("enrolled_at", desc=True)
        .limit(10)
        .execute()
    )
    enrolled_courses = enrollments_res.data or []

    # Focus topics from topic_performance
    focus_res = (
        await supabase.table(TOPIC_PERFORMANCE_TABLE)
        .select("topic_id, mastery_level, last_quiz_score, phase_topics(title, phase_id)")
        .eq("user_id", user_id)
        .eq("is_completed", False)
        .order("last_quiz_score", desc=False)
        .limit(5)
        .execute()
    )
    focus_topics: list[FocusTopic] = []
    for f in (focus_res.data or []):
        pt = f.get("phase_topics") or {}
        focus_topics.append(
            FocusTopic(
                topic_id=str(f.get("topic_id")),
                topic_title=pt.get("title") or "Core Topic",
                mastery_level=f.get("mastery_level") or "needs_practice",
                last_quiz_score=f.get("last_quiz_score"),
            )
        )

    # Recent quizzes
    recent_quiz_res = (
        await supabase.table(QUIZ_ATTEMPTS_TABLE)
        .select("*, quizzes(title)")
        .eq("user_id", user_id)
        .order("completed_at", desc=True)
        .limit(5)
        .execute()
    )
    recent_quizzes = recent_quiz_res.data or []

    return StudentDashboard(
        profile=profile,
        stats=stats,
        enrolled_courses=enrolled_courses,
        focus_topics=focus_topics,
        recent_quizzes=recent_quizzes,
    )
