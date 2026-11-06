"""
Course Management, AI Recommendations, and Faculty Cohort Assignments.
"""

import asyncio
from datetime import datetime, timezone
import json
import logging
from typing import Any
from fastapi import HTTPException, status
from google import genai
from google.genai import types
from supabase import AsyncClient
from app.core.config import settings
from app.courses.schemas import (
    AssignCourseRequest,
    AssignmentStats,
    Course,
    CourseEnrollment,
    CourseRecommendation,
    CreateCustomCourse,
    FacultyCourseAssignment,
)
from app.database.models import (
    COURSE_ENROLLMENTS_TABLE,
    COURSE_RECOMMENDATIONS_TABLE,
    COURSES_TABLE,
    FACULTY_COURSE_ASSIGNMENTS_TABLE,
    STUDENT_PROFILES_TABLE,
)

logger = logging.getLogger(__name__)


def get_gemini_client() -> genai.Client:
    """Initialize Google GenAI Client with configured API key."""
    return genai.Client(api_key=settings.GEMINI_API_KEY)


async def call_gemini_with_retry(
    prompt: str,
    retries: int = 3,
    delay: float = 2.0,
    response_mime_type: str = "application/json",
) -> str:
    """Execute Gemini prompt with exponential backoff on rate limits."""
    if not settings.GEMINI_API_KEY:
        logger.warning("GEMINI_API_KEY is not configured. Returning fallback response.")
        return ""

    client = get_gemini_client()
    for attempt in range(retries):
        try:
            # Run blocking SDK call in threadpool
            gen_config = (
                types.GenerateContentConfig(response_mime_type=response_mime_type, temperature=0.3)
                if response_mime_type
                else types.GenerateContentConfig(temperature=0.3)
            )
            response = await asyncio.to_thread(
                client.models.generate_content,
                model=settings.GEMINI_MODEL,
                contents=prompt,
                config=gen_config,
            )
            return response.text or ""
        except Exception as exc:
            logger.warning("Gemini attempt %s failed: %s", attempt + 1, exc)
            if attempt < retries - 1:
                await asyncio.sleep(delay * (2 ** attempt))
            else:
                logger.error("All Gemini retry attempts exhausted.")
                raise HTTPException(
                    status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                    detail=f"AI service temporarily unavailable: {str(exc)}",
                )
    return ""


async def get_course_recommendations(
    user_id: str,
    supabase: AsyncClient,
) -> list[CourseRecommendation]:
    """Generate or retrieve AI-powered course recommendations personalized to the student.

    Args:
        user_id: UUID of student.
        supabase: Active Supabase AsyncClient.

    Returns:
        List of CourseRecommendation models.
    """
    # 1. Check existing active recommendations
    cached = (
        await supabase.table(COURSE_RECOMMENDATIONS_TABLE)
        .select("*")
        .eq("user_id", user_id)
        .eq("is_dismissed", False)
        .order("created_at", desc=True)
        .limit(5)
        .execute()
    )
    if cached.data:
        return [CourseRecommendation.model_validate(item) for item in cached.data]

    # 2. Fetch student profile context
    profile_res = (
        await supabase.table(STUDENT_PROFILES_TABLE)
        .select("*")
        .eq("user_id", user_id)
        .limit(1)
        .execute()
    )
    profile = profile_res.data[0] if profile_res.data else {}

    branch = profile.get("branch", "Computer Science & Engineering")
    semester = profile.get("semester", 4)
    interests = profile.get("learning_interests", ["Artificial Intelligence", "Web Development"])

    # 3. Formulate Gemini prompt for structured JSON course recommendations
    prompt = f"""
You are an expert academic advisor for engineering undergraduate students.
Based on the following student academic background, recommend 4 highly relevant courses:
Branch: {branch}
Semester: {semester}
Interests: {', '.join(interests) if isinstance(interests, list) else interests}

Return a valid JSON array containing exactly 4 course objects with these keys:
- course_title: string (e.g. "Distributed Systems & Cloud Architecture")
- description: string (2-3 sentences overview)
- category: string (e.g. "Cloud Computing", "AI/ML", "Cybersecurity", "Systems")
- difficulty: string ("Beginner", "Intermediate", or "Advanced")
- reason: string (why this matches their branch, semester, and interests)
- match_score: float between 0.85 and 0.99
"""

    recommendations: list[dict[str, Any]] = []
    try:
        raw_json = await call_gemini_with_retry(prompt)
        if raw_json:
            parsed = json.loads(raw_json)
            if isinstance(parsed, list):
                recommendations = parsed
            elif isinstance(parsed, dict) and "courses" in parsed:
                recommendations = parsed["courses"]
    except Exception as e:
        logger.warning("Gemini course recommendation failed: %s. Using default curriculum.", e)
        recommendations = [
            {
                "course_title": f"Full-Stack Modern Web Engineering for {branch}",
                "description": "Master end-to-end cloud applications using FastAPI, React, and scalable cloud databases.",
                "category": "Web Development",
                "difficulty": "Intermediate",
                "reason": "Essential practical foundation aligned with industry software standards.",
                "match_score": 0.95,
            },
            {
                "course_title": "Applied Machine Learning & Neural Networks",
                "description": "Understand core machine learning algorithms, model evaluation, and modern deep learning.",
                "category": "Artificial Intelligence",
                "difficulty": "Intermediate",
                "reason": "Directly matches high industry demand and your expressed AI interests.",
                "match_score": 0.92,
            },
            {
                "course_title": "Data Structures & Algorithmic Problem Solving",
                "description": "Develop mastery in trees, graphs, dynamic programming, and competitive interview skills.",
                "category": "Computer Science Core",
                "difficulty": "Intermediate",
                "reason": "Critical for campus placements and technical evaluations at Siddhartha Academy.",
                "match_score": 0.97,
            },
        ]

    # Save to database and return
    result_models: list[CourseRecommendation] = []
    for rec in recommendations:
        payload = {
            "user_id": user_id,
            "course_title": rec.get("course_title"),
            "description": rec.get("description"),
            "category": rec.get("category", "General"),
            "difficulty": rec.get("difficulty", "Intermediate"),
            "reason": rec.get("reason", "Curriculum alignment"),
            "match_score": rec.get("match_score", 0.90),
            "is_dismissed": False,
        }
        res = await supabase.table(COURSE_RECOMMENDATIONS_TABLE).insert(payload).execute()
        if res.data:
            result_models.append(CourseRecommendation.model_validate(res.data[0]))

    return result_models


async def create_custom_course(
    user_id: str,
    data: CreateCustomCourse,
    supabase: AsyncClient,
) -> Course:
    """Create a customized course and automatically enroll the creator.

    Args:
        user_id: UUID of user creating the course.
        data: CreateCustomCourse schema.
        supabase: Active Supabase AsyncClient.

    Returns:
        Created Course model.
    """
    course_payload = {
        "title": data.title,
        "description": data.description or f"Customized learning course for {data.title}",
        "category": data.category or "Custom",
        "difficulty": data.difficulty,
        "is_custom": True,
        "created_by": user_id,
    }
    res = await supabase.table(COURSES_TABLE).insert(course_payload).execute()
    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to create custom course.",
        )

    new_course = res.data[0]
    course_id = new_course["id"]

    # Automatically enroll creator
    await enroll_in_course(user_id=user_id, course_id=course_id, supabase=supabase)

    return Course.model_validate(new_course)


async def enroll_in_course(
    user_id: str,
    course_id: str,
    supabase: AsyncClient,
) -> CourseEnrollment:
    """Enroll a student into a course if not already enrolled.

    Args:
        user_id: UUID of student.
        course_id: UUID of course.
        supabase: Active Supabase AsyncClient.

    Returns:
        CourseEnrollment model.
    """
    # Verify course exists
    course_check = (
        await supabase.table(COURSES_TABLE).select("*").eq("id", course_id).limit(1).execute()
    )
    if not course_check.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course does not exist.",
        )

    # Check existing enrollment
    existing = (
        await supabase.table(COURSE_ENROLLMENTS_TABLE)
        .select("*, courses(*)")
        .eq("user_id", user_id)
        .eq("course_id", course_id)
        .limit(1)
        .execute()
    )
    if existing.data:
        return CourseEnrollment.model_validate(existing.data[0])

    insert_payload = {
        "user_id": user_id,
        "course_id": course_id,
        "status": "enrolled",
        "progress_percentage": 0.0,
    }
    res = (
        await supabase.table(COURSE_ENROLLMENTS_TABLE)
        .insert(insert_payload)
        .execute()
    )
    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to enroll in course.",
        )

    data = res.data[0]
    data["course"] = course_check.data[0]
    return CourseEnrollment.model_validate(data)


async def get_enrolled_courses(
    user_id: str,
    supabase: AsyncClient,
) -> list[CourseEnrollment]:
    """Retrieve all courses in which the student is enrolled.

    Args:
        user_id: UUID of student.
        supabase: Active Supabase AsyncClient.

    Returns:
        List of CourseEnrollment models with course metadata.
    """
    res = (
        await supabase.table(COURSE_ENROLLMENTS_TABLE)
        .select("*, courses(*)")
        .eq("user_id", user_id)
        .order("enrolled_at", desc=True)
        .execute()
    )
    return [CourseEnrollment.model_validate(row) for row in (res.data or [])]


async def assign_course_to_cohort(
    faculty_id: str,
    data: AssignCourseRequest,
    supabase: AsyncClient,
) -> FacultyCourseAssignment:
    """Assign a course to all students within an academic year, branch, and semester cohort.

    Args:
        faculty_id: UUID of assigning faculty.
        data: AssignCourseRequest details.
        supabase: Active Supabase AsyncClient.

    Returns:
        FacultyCourseAssignment record.
    """
    # 1. Record assignment
    assignment_payload = {
        "faculty_id": faculty_id,
        "course_id": data.course_id,
        "academic_year": data.academic_year,
        "branch": data.branch,
        "semester": data.semester,
        "due_date": data.due_date.isoformat() if isinstance(data.due_date, datetime) else data.due_date,
    }
    assign_res = (
        await supabase.table(FACULTY_COURSE_ASSIGNMENTS_TABLE)
        .insert(assignment_payload)
        .execute()
    )
    if not assign_res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to record faculty course assignment.",
        )
    assignment = assign_res.data[0]

    # 2. Query matching students
    students_res = (
        await supabase.table(STUDENT_PROFILES_TABLE)
        .select("user_id")
        .eq("academic_year", data.academic_year)
        .eq("semester", data.semester)
        .ilike("branch", f"%{data.branch}%")
        .execute()
    )

    # 3. Enroll students who are not yet enrolled
    for student in (students_res.data or []):
        s_user_id = student["user_id"]
        try:
            await enroll_in_course(user_id=s_user_id, course_id=data.course_id, supabase=supabase)
        except Exception:
            pass

    return FacultyCourseAssignment.model_validate(assignment)


async def get_faculty_assignments(
    faculty_id: str,
    supabase: AsyncClient,
) -> list[FacultyCourseAssignment]:
    """Retrieve all course assignments initiated by a faculty member.

    Args:
        faculty_id: UUID of faculty.
        supabase: Active Supabase AsyncClient.

    Returns:
        List of FacultyCourseAssignment records.
    """
    res = (
        await supabase.table(FACULTY_COURSE_ASSIGNMENTS_TABLE)
        .select("*, courses(*)")
        .eq("faculty_id", faculty_id)
        .order("assigned_at", desc=True)
        .execute()
    )
    return [FacultyCourseAssignment.model_validate(row) for row in (res.data or [])]


async def get_assignment_stats(
    assignment_id: str,
    supabase: AsyncClient,
) -> AssignmentStats:
    """Calculate enrollment, completion, and progress statistics for an assignment.

    Args:
        assignment_id: UUID of faculty assignment.
        supabase: Active Supabase AsyncClient.

    Returns:
        AssignmentStats model.
    """
    assign_res = (
        await supabase.table(FACULTY_COURSE_ASSIGNMENTS_TABLE)
        .select("*, courses(title)")
        .eq("id", assignment_id)
        .limit(1)
        .execute()
    )
    if not assign_res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Assignment not found.",
        )

    assignment = assign_res.data[0]
    course_title = (assignment.get("courses") or {}).get("title") or "Assigned Course"
    target_group = f"Year {assignment['academic_year']} - {assignment['branch']} (Sem {assignment['semester']})"

    # Find total cohort students
    cohort_res = (
        await supabase.table(STUDENT_PROFILES_TABLE)
        .select("user_id")
        .eq("academic_year", assignment["academic_year"])
        .eq("semester", assignment["semester"])
        .ilike("branch", f"%{assignment['branch']}%")
        .execute()
    )
    student_user_ids = [s["user_id"] for s in (cohort_res.data or [])]
    total_assigned = len(student_user_ids)

    if not student_user_ids:
        return AssignmentStats(
            assignment_id=assignment_id,
            course_title=course_title,
            target_group=target_group,
            total_assigned_students=0,
            total_enrolled=0,
            total_completed=0,
            average_progress=0.0,
        )

    # Enrollments for this course among cohort
    enrollments_res = (
        await supabase.table(COURSE_ENROLLMENTS_TABLE)
        .select("progress_percentage, status")
        .eq("course_id", assignment["course_id"])
        .in_("user_id", student_user_ids)
        .execute()
    )
    enrollments = enrollments_res.data or []
    total_enrolled = len(enrollments)
    total_completed = sum(1 for e in enrollments if (e.get("progress_percentage") or 0) >= 100)
    avg_progress = (
        round(sum(float(e.get("progress_percentage") or 0) for e in enrollments) / total_enrolled, 1)
        if total_enrolled > 0
        else 0.0
    )

    return AssignmentStats(
        assignment_id=assignment_id,
        course_title=course_title,
        target_group=target_group,
        total_assigned_students=total_assigned,
        total_enrolled=total_enrolled,
        total_completed=total_completed,
        average_progress=avg_progress,
    )


async def dismiss_recommendation(
    user_id: str,
    recommendation_id: str,
    supabase: AsyncClient,
) -> dict[str, Any]:
    """Dismiss a course recommendation so it no longer appears in the feed.

    Args:
        user_id: UUID of student.
        recommendation_id: UUID of recommendation.
        supabase: Active Supabase AsyncClient.

    Returns:
        Status message dict.
    """
    await (
        supabase.table(COURSE_RECOMMENDATIONS_TABLE)
        .update({"is_dismissed": True})
        .eq("id", recommendation_id)
        .eq("user_id", user_id)
        .execute()
    )
    return {"message": "Course recommendation dismissed.", "success": True}
