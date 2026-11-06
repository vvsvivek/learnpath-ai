"""
Context Aggregation for AI Chatbot Mentors (Student and Faculty Contexts).
"""

from typing import Any
from supabase import AsyncClient
from app.database.models import (
    COURSE_ENROLLMENTS_TABLE,
    FACULTY_PROFILES_TABLE,
    QUIZ_ATTEMPTS_TABLE,
    STUDENT_PROFILES_TABLE,
    STUDY_SESSIONS_TABLE,
    TOPIC_PERFORMANCE_TABLE,
)
from app.faculty.schemas import StudentFilter
from app.faculty.service import get_faculty_students


async def build_student_context(user_id: str, supabase: AsyncClient) -> str:
    """Gather complete student academic profile, course progress, quiz grades, and focus topics.

    Args:
        user_id: UUID of student.
        supabase: Active Supabase AsyncClient.

    Returns:
        Structured string context for Gemini prompt injection.
    """
    # 1. Profile
    profile_res = await supabase.table(STUDENT_PROFILES_TABLE).select("*").eq("user_id", user_id).limit(1).execute()
    profile = profile_res.data[0] if profile_res.data else {}

    full_name = profile.get("full_name", "Student")
    branch = profile.get("branch", "Engineering")
    academic_year = profile.get("academic_year", 1)
    semester = profile.get("semester", 1)
    cgpa = profile.get("cgpa", "N/A")
    interests = profile.get("learning_interests", [])

    # 2. Enrolled courses and progress
    enrollments_res = (
        await supabase.table(COURSE_ENROLLMENTS_TABLE)
        .select("progress_percentage, courses(title)")
        .eq("user_id", user_id)
        .execute()
    )
    courses_info = [
        f"- {(e.get('courses') or {}).get('title', 'Course')}: {e.get('progress_percentage', 0)}% completed"
        for e in (enrollments_res.data or [])
    ]

    # 3. Recent Quiz Scores
    quiz_res = (
        await supabase.table(QUIZ_ATTEMPTS_TABLE)
        .select("percentage, quizzes(title)")
        .eq("user_id", user_id)
        .order("completed_at", desc=True)
        .limit(3)
        .execute()
    )
    quiz_info = [
        f"- {(q.get('quizzes') or {}).get('title', 'Quiz')}: {q.get('percentage')}%"
        for q in (quiz_res.data or [])
    ]

    # 4. Weak / Need Practice Topics
    weak_res = (
        await supabase.table(TOPIC_PERFORMANCE_TABLE)
        .select("last_quiz_score, phase_topics(title)")
        .eq("user_id", user_id)
        .eq("mastery_level", "needs_practice")
        .limit(5)
        .execute()
    )
    weak_topics = [
        (w.get("phase_topics") or {}).get("title", "Topic")
        for w in (weak_res.data or [])
    ]

    # 5. Study Time
    study_res = await supabase.table(STUDY_SESSIONS_TABLE).select("duration_minutes").eq("user_id", user_id).execute()
    total_study_minutes = sum(s.get("duration_minutes") or 0 for s in (study_res.data or []))

    context = f"""
STUDENT PROFILE & CONTEXT:
- Name: {full_name}
- Department/Branch: {branch} (Year {academic_year}, Semester {semester})
- Academic CGPA: {cgpa}
- Expressed Learning Interests: {', '.join(interests) if isinstance(interests, list) else interests}
- Total Study Time Logged: {round(total_study_minutes / 60.0, 1)} hours

CURRENT ENROLLED COURSES:
{chr(10).join(courses_info) if courses_info else 'No current enrollments.'}

RECENT QUIZ PERFORMANCE:
{chr(10).join(quiz_info) if quiz_info else 'No recent quiz attempts.'}

TOPICS CURRENTLY REQUIRING ATTENTION / PRACTICE:
{', '.join(weak_topics) if weak_topics else 'No struggling topics detected.'}
"""
    return context.strip()


async def build_faculty_context(
    user_id: str,
    supabase: AsyncClient,
    filters: StudentFilter | None = None,
) -> str:
    """Gather institutional metrics, cohort distribution, and department statistics for faculty chat.

    Args:
        user_id: UUID of faculty member.
        supabase: Active Supabase AsyncClient.
        filters: Optional filter parameters.

    Returns:
        Structured string context for Gemini prompt injection.
    """
    profile_res = await supabase.table(FACULTY_PROFILES_TABLE).select("*").eq("user_id", user_id).limit(1).execute()
    profile = profile_res.data[0] if profile_res.data else {}

    faculty_name = profile.get("full_name", "Professor")
    dept = profile.get("department", "Engineering")

    active_filter = filters or StudentFilter(branch=dept, institutional_only=True)
    students = await get_faculty_students(filters=active_filter, supabase=supabase)

    total_students = len(students)
    high_count = sum(1 for s in students if s.performance_tier == "high_performing")
    needs_support = sum(1 for s in students if s.performance_tier == "needs_support")
    quizzes = [s.avg_quiz_score for s in students if s.avg_quiz_score > 0]
    avg_score = round(sum(quizzes) / len(quizzes), 1) if quizzes else 0.0

    context = f"""
FACULTY & INSTITUTIONAL CONTEXT:
- Faculty Member: {faculty_name}
- Department: {dept}
- Target Student Cohort Size: {total_students} students
- Class Average Quiz Performance: {avg_score}%
- High Performing Students: {high_count}
- Students Requiring Additional Pedagogical Support: {needs_support}
- Institutional Email Domain: @vrsec.ac.in (Siddhartha Academy)
"""
    return context.strip()
