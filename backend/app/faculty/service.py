"""
Faculty Analytics and Student Management Service.
"""

from typing import Any
from fastapi import HTTPException, status
from supabase import AsyncClient
from app.core.config import settings
from app.database.models import (
    COURSE_ENROLLMENTS_TABLE,
    COURSES_TABLE,
    FACULTY_COURSE_ASSIGNMENTS_TABLE,
    QUIZ_ATTEMPTS_TABLE,
    STUDENT_PROFILES_TABLE,
    STUDY_SESSIONS_TABLE,
    TOPIC_PERFORMANCE_TABLE,
    USERS_TABLE,
)
from app.faculty.schemas import (
    FacultyDashboard,
    StudentAnalytics,
    StudentCategory,
    StudentFilter,
)


def compute_performance_tier(avg_quiz: float, cgpa: float | None) -> str:
    """Determine performance tier based on quiz average and CGPA."""
    if avg_quiz >= 80.0 or (cgpa is not None and cgpa >= 8.5):
        return "high_performing"
    if (avg_quiz > 0 and avg_quiz < 60.0) or (cgpa is not None and cgpa < 6.0):
        return "needs_support"
    return "average"


async def get_faculty_students(
    filters: StudentFilter,
    supabase: AsyncClient,
) -> list[StudentAnalytics]:
    """Retrieve students matching institutional filters with aggregated performance metrics.

    Args:
        filters: StudentFilter criteria (year, branch, semester, institutional_only).
        supabase: Active Supabase AsyncClient.

    Returns:
        List of StudentAnalytics records.
    """
    # Fetch profiles joined with users table
    query = supabase.table(STUDENT_PROFILES_TABLE).select("*, users(email, is_active)")

    if filters.academic_year:
        query = query.eq("academic_year", filters.academic_year)
    if filters.branch:
        query = query.ilike("branch", f"%{filters.branch}%")
    if filters.semester:
        query = query.eq("semester", filters.semester)

    res = await query.execute()
    profiles = res.data or []

    institutional_domain = settings.STUDENT_INSTITUTIONAL_DOMAIN.strip().lower()
    if not institutional_domain.startswith("@"):
        institutional_domain = f"@{institutional_domain}"

    students_list: list[StudentAnalytics] = []

    for p in profiles:
        user_info = p.get("users") or {}
        email = (user_info.get("email") or "").lower()

        # Institutional filter check
        if filters.institutional_only and not email.endswith(institutional_domain):
            continue

        user_id = p["user_id"]

        # Aggregate course enrollments
        enrollments_res = (
            await supabase.table(COURSE_ENROLLMENTS_TABLE)
            .select("id", count="exact")
            .eq("user_id", user_id)
            .execute()
        )
        courses_count = enrollments_res.count or len(enrollments_res.data or [])

        # Aggregate quiz performance
        quiz_res = (
            await supabase.table(QUIZ_ATTEMPTS_TABLE)
            .select("percentage")
            .eq("user_id", user_id)
            .execute()
        )
        attempts = quiz_res.data or []
        if attempts:
            avg_quiz = round(
                sum(float(a.get("percentage") or 0) for a in attempts) / len(attempts), 1
            )
        else:
            avg_quiz = 0.0

        # Aggregate study hours
        study_res = (
            await supabase.table(STUDY_SESSIONS_TABLE)
            .select("duration_minutes")
            .eq("user_id", user_id)
            .execute()
        )
        total_mins = sum(s.get("duration_minutes") or 0 for s in (study_res.data or []))
        study_hours = round(total_mins / 60.0, 1)

        cgpa = float(p["cgpa"]) if p.get("cgpa") is not None else None
        tier = compute_performance_tier(avg_quiz, cgpa)

        students_list.append(
            StudentAnalytics(
                student_id=str(p["id"]),
                user_id=str(user_id),
                full_name=p.get("full_name") or "Student",
                email=email,
                branch=p.get("branch") or "General",
                academic_year=p.get("academic_year") or 1,
                semester=p.get("semester") or 1,
                cgpa=cgpa,
                courses_enrolled=courses_count,
                avg_quiz_score=avg_quiz,
                study_hours=study_hours,
                performance_tier=tier,  # type: ignore[arg-type]
            )
        )

    return students_list


def categorize_students_list(students: list[StudentAnalytics]) -> list[StudentCategory]:
    """Group students into analytical categories for faculty action."""
    high = [s for s in students if s.performance_tier == "high_performing"]
    support = [s for s in students if s.performance_tier == "needs_support"]
    average = [s for s in students if s.performance_tier == "average"]

    return [
        StudentCategory(
            category_name="High Performing",
            student_count=len(high),
            students=high,
        ),
        StudentCategory(
            category_name="Needs Support",
            student_count=len(support),
            students=support,
        ),
        StudentCategory(
            category_name="Average Progress",
            student_count=len(average),
            students=average,
        ),
    ]


async def get_faculty_dashboard(user_id: str, supabase: AsyncClient) -> FacultyDashboard:
    """Consolidate institutional metrics, student progress, and assignments for faculty dashboard.

    Args:
        user_id: UUID of faculty member.
        supabase: Active Supabase AsyncClient.

    Returns:
        FacultyDashboard model.
    """
    # Fetch all institutional students
    all_students = await get_faculty_students(
        filters=StudentFilter(institutional_only=False),
        supabase=supabase,
    )

    institutional_domain = settings.STUDENT_INSTITUTIONAL_DOMAIN.strip().lower()
    institutional_students = [
        s for s in all_students if s.email.endswith(institutional_domain)
    ]

    # Active courses in the platform
    courses_res = await supabase.table(COURSES_TABLE).select("id", count="exact").execute()
    active_courses_count = courses_res.count or len(courses_res.data or [])

    # Class performance average
    quiz_scores = [s.avg_quiz_score for s in all_students if s.avg_quiz_score > 0]
    avg_performance = round(sum(quiz_scores) / len(quiz_scores), 1) if quiz_scores else 0.0

    high_count = sum(1 for s in all_students if s.performance_tier == "high_performing")
    support_count = sum(1 for s in all_students if s.performance_tier == "needs_support")

    # Recent course assignments made by this faculty
    assignments_res = (
        await supabase.table(FACULTY_COURSE_ASSIGNMENTS_TABLE)
        .select("*, courses(title)")
        .eq("faculty_id", user_id)
        .order("assigned_at", desc=True)
        .limit(5)
        .execute()
    )

    return FacultyDashboard(
        total_students=len(all_students),
        institutional_students_count=len(institutional_students),
        active_courses=active_courses_count,
        average_class_performance=avg_performance,
        high_performing_count=high_count,
        needs_support_count=support_count,
        students_list=all_students,
        recent_assignments=assignments_res.data or [],
    )


async def get_student_detail(student_id: str, supabase: AsyncClient) -> dict[str, Any]:
    """Retrieve in-depth analytics, course enrollments, and quiz attempts for a specific student.

    Args:
        student_id: Profile ID or User ID of student.
        supabase: Active Supabase AsyncClient.

    Returns:
        Detailed dictionary of student records.
    """
    # Find profile
    profile_res = (
        await supabase.table(STUDENT_PROFILES_TABLE)
        .select("*, users(email, is_active)")
        .or_(f"id.eq.{student_id},user_id.eq.{student_id}")
        .limit(1)
        .execute()
    )
    if not profile_res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Student not found.",
        )

    profile = profile_res.data[0]
    user_id = profile["user_id"]

    # Enrollments
    enrollments = (
        await supabase.table(COURSE_ENROLLMENTS_TABLE)
        .select("*, courses(title, category, difficulty)")
        .eq("user_id", user_id)
        .execute()
    ).data or []

    # Quiz history
    quizzes = (
        await supabase.table(QUIZ_ATTEMPTS_TABLE)
        .select("*, quizzes(title)")
        .eq("user_id", user_id)
        .order("completed_at", desc=True)
        .execute()
    ).data or []

    # Weak / focus topics
    topics = (
        await supabase.table(TOPIC_PERFORMANCE_TABLE)
        .select("*, phase_topics(title)")
        .eq("user_id", user_id)
        .execute()
    ).data or []

    return {
        "profile": profile,
        "enrollments": enrollments,
        "quiz_history": quizzes,
        "topic_performance": topics,
    }
