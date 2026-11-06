"""
Student and Faculty Analytics Reporting Service.
"""

import json
import logging
from typing import Any
from fastapi import HTTPException, status
from supabase import AsyncClient
from app.core.config import settings
from app.courses.service import call_gemini_with_retry
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
from app.faculty.schemas import StudentFilter
from app.faculty.service import get_faculty_students
from app.reports.schemas import (
    CourseAnalytics,
    FacultyReport,
    QuizAnalytics,
    StudentReport,
)

logger = logging.getLogger(__name__)


async def get_student_report(
    user_id: str,
    supabase: AsyncClient,
) -> StudentReport:
    """Generate a comprehensive academic report with AI-driven learning insights for a student.

    Args:
        user_id: UUID of student.
        supabase: Active Supabase AsyncClient.

    Returns:
        StudentReport model.
    """
    # 1. Fetch student profile
    profile_res = (
        await supabase.table(STUDENT_PROFILES_TABLE)
        .select("*")
        .eq("user_id", user_id)
        .limit(1)
        .execute()
    )
    if not profile_res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Student profile not found.",
        )
    profile = profile_res.data[0]
    student_name = profile.get("full_name") or "Student"
    branch = profile.get("branch") or "General"
    year = profile.get("academic_year") or 1

    # 2. Total study hours
    study_res = await supabase.table(STUDY_SESSIONS_TABLE).select("duration_minutes").eq("user_id", user_id).execute()
    total_minutes = sum(s.get("duration_minutes") or 0 for s in (study_res.data or []))
    total_hours = round(total_minutes / 60.0, 1)

    # 3. Enrolled courses analytics
    enrollments_res = (
        await supabase.table(COURSE_ENROLLMENTS_TABLE)
        .select("*, courses(title)")
        .eq("user_id", user_id)
        .execute()
    )
    enrolled_courses: list[CourseAnalytics] = []
    total_prog_sum = 0.0
    for e in (enrollments_res.data or []):
        prog = float(e.get("progress_percentage") or 0.0)
        total_prog_sum += prog
        course_title = (e.get("courses") or {}).get("title") or "Course"
        enrolled_courses.append(
            CourseAnalytics(
                course_id=str(e["course_id"]),
                course_title=course_title,
                total_enrolled=1,
                completion_rate=100.0 if prog >= 100 else 0.0,
                average_progress=prog,
            )
        )
    overall_progress = (
        round(total_prog_sum / len(enrolled_courses), 1) if enrolled_courses else 0.0
    )

    # 4. Quiz history
    quizzes_res = (
        await supabase.table(QUIZ_ATTEMPTS_TABLE)
        .select("*, quizzes(title)")
        .eq("user_id", user_id)
        .order("completed_at", desc=True)
        .execute()
    )
    quiz_history: list[QuizAnalytics] = []
    for q in (quizzes_res.data or []):
        pct = float(q.get("percentage") or 0.0)
        q_title = (q.get("quizzes") or {}).get("title") or "Assessment"
        quiz_history.append(
            QuizAnalytics(
                quiz_id=str(q["quiz_id"]),
                quiz_title=q_title,
                total_attempts=1,
                average_score=pct,
                pass_rate=100.0 if pct >= 60.0 else 0.0,
            )
        )

    # 5. Strong and weak topics
    topics_res = (
        await supabase.table(TOPIC_PERFORMANCE_TABLE)
        .select("*, phase_topics(title)")
        .eq("user_id", user_id)
        .execute()
    )
    strong_topics: list[str] = []
    weak_topics: list[str] = []
    for t in (topics_res.data or []):
        title = (t.get("phase_topics") or {}).get("title") or "Topic"
        score = t.get("last_quiz_score")
        if (score is not None and score >= 80.0) or t.get("mastery_level") == "mastered":
            strong_topics.append(title)
        elif score is not None and score < 60.0:
            weak_topics.append(title)

    # 6. AI Insights via Gemini
    prompt = f"""
Provide a concise, encouraging academic diagnosis (3-4 sentences) for student: {student_name}
Branch: {branch}, Year: {year}
Completed Study Hours: {total_hours}h
Overall Course Completion: {overall_progress}%
Strong Topics: {', '.join(strong_topics[:5]) if strong_topics else 'None identified yet'}
Topics Needing Review: {', '.join(weak_topics[:5]) if weak_topics else 'None currently identified'}

Give specific suggestions on where to focus next.
"""
    ai_insights = ""
    try:
        raw_insights = await call_gemini_with_retry(prompt, response_mime_type="text/plain")
        ai_insights = raw_insights.strip() if raw_insights else ""
    except Exception as exc:
        logger.warning("Could not generate AI insights: %s", exc)
        ai_insights = (
            f"{student_name} is making steady progress with {total_hours} recorded study hours. "
            f"Focus on solidifying core topics in your {branch} curriculum and consistently taking phase assessments."
        )

    return StudentReport(
        student_id=str(profile["id"]),
        student_name=student_name,
        branch=branch,
        academic_year=year,
        overall_progress=overall_progress,
        total_study_hours=total_hours,
        enrolled_courses=enrolled_courses,
        quiz_history=quiz_history,
        strong_topics=strong_topics[:8],
        weak_topics=weak_topics[:8],
        ai_insights=ai_insights,
    )


async def get_faculty_report(
    filters: StudentFilter,
    supabase: AsyncClient,
) -> FacultyReport:
    """Generate institutional faculty summary report across students with AI pedagogical advice.

    Args:
        filters: StudentFilter criteria.
        supabase: Active Supabase AsyncClient.

    Returns:
        FacultyReport model.
    """
    students = await get_faculty_students(filters=filters, supabase=supabase)
    total_students = len(students)
    department = filters.branch or "All Departments"

    institutional_domain = settings.STUDENT_INSTITUTIONAL_DOMAIN.strip().lower()
    institutional_count = sum(1 for s in students if s.email.endswith(institutional_domain))
    institutional_pct = (
        round((institutional_count / total_students) * 100.0, 1) if total_students > 0 else 0.0
    )

    cgpas = [s.cgpa for s in students if s.cgpa is not None]
    avg_cgpa = round(sum(cgpas) / len(cgpas), 2) if cgpas else 0.0

    quiz_scores = [s.avg_quiz_score for s in students if s.avg_quiz_score > 0]
    avg_quiz = round(sum(quiz_scores) / len(quiz_scores), 1) if quiz_scores else 0.0

    high_count = sum(1 for s in students if s.performance_tier == "high_performing")
    support_count = sum(1 for s in students if s.performance_tier == "needs_support")
    avg_count = total_students - (high_count + support_count)

    performance_distribution = {
        "high_performing": high_count,
        "average": avg_count,
        "needs_support": support_count,
    }

    # Fetch courses summary
    courses_res = (
        await supabase.table(COURSES_TABLE)
        .select("id, title")
        .limit(10)
        .execute()
    )
    courses_overview: list[CourseAnalytics] = []
    for c in (courses_res.data or []):
        c_id = c["id"]
        enr_res = await supabase.table(COURSE_ENROLLMENTS_TABLE).select("progress_percentage").eq("course_id", c_id).execute()
        enr_data = enr_res.data or []
        tot = len(enr_data)
        avg_prog = (
            round(sum(float(x.get("progress_percentage") or 0.0) for x in enr_data) / tot, 1)
            if tot > 0
            else 0.0
        )
        comp_rate = (
            round(sum(1 for x in enr_data if (x.get("progress_percentage") or 0.0) >= 100) / tot * 100.0, 1)
            if tot > 0
            else 0.0
        )
        courses_overview.append(
            CourseAnalytics(
                course_id=str(c_id),
                course_title=c["title"],
                total_enrolled=tot,
                completion_rate=comp_rate,
                average_progress=avg_prog,
            )
        )

    # Gemini Recommendations for faculty
    prompt = f"""
You are an institutional academic consultant evaluating student performance for {department}.
Summary Statistics:
Total Students Analyzed: {total_students}
Average CGPA: {avg_cgpa}
Average Phase Quiz Score: {avg_quiz}%
Students Needing Support: {support_count}
High-Performing Students: {high_count}

Provide 3 actionable curriculum interventions or pedagogical recommendations for the faculty.
Return as a valid JSON array of strings: ["recommendation 1", "recommendation 2", "recommendation 3"]
"""
    recommendations: list[str] = []
    try:
        raw_json = await call_gemini_with_retry(prompt)
        if raw_json:
            parsed = json.loads(raw_json)
            if isinstance(parsed, list):
                recommendations = [str(r) for r in parsed]
    except Exception as exc:
        logger.warning("Faculty report recommendation generation failed: %s", exc)
        recommendations = [
            f"Introduce targeted hands-on tutorial sessions for the {support_count} students identified in the needs-support tier.",
            "Incorporate continuous formative micro-assessments at the conclusion of each learning phase.",
            "Recognize high-performing students through advanced optional capstone research challenges.",
        ]

    return FacultyReport(
        department=department,
        academic_year=filters.academic_year,
        total_students=total_students,
        institutional_percentage=institutional_pct,
        avg_cgpa=avg_cgpa,
        avg_quiz_score=avg_quiz,
        courses_overview=courses_overview,
        performance_distribution=performance_distribution,
        key_recommendations=recommendations,
    )
