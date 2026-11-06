"""
AI-Powered Learning Path Generation and Progress Tracking Service.
"""

from datetime import datetime, timezone
import json
import logging
from typing import Any
from fastapi import HTTPException, status
from supabase import AsyncClient
from app.courses.service import call_gemini_with_retry
from app.database.models import (
    COURSE_ENROLLMENTS_TABLE,
    COURSES_TABLE,
    LEARNING_PHASES_TABLE,
    PHASE_TOPICS_TABLE,
    RESOURCES_TABLE,
    STUDENT_PROFILES_TABLE,
    TOPIC_PERFORMANCE_TABLE,
)
from app.learning_paths.schemas import (
    LearningPath,
    LearningPhase,
    PhaseProgress,
    PhaseTopic,
    Resource,
)

logger = logging.getLogger(__name__)


async def generate_learning_path(
    course_id: str,
    user_id: str,
    supabase: AsyncClient,
) -> LearningPath:
    """Generate a structured 4-phase learning path with topics and curated resources using Gemini.

    If the learning path has already been created for this course, retrieves the existing path.

    Args:
        course_id: UUID of course.
        user_id: UUID of student user.
        supabase: Active Supabase AsyncClient.

    Returns:
        LearningPath model.
    """
    # 1. Check if path already exists
    existing_phases = (
        await supabase.table(LEARNING_PHASES_TABLE)
        .select("id")
        .eq("course_id", course_id)
        .execute()
    )
    if existing_phases.data:
        return await get_learning_path(course_id=course_id, user_id=user_id, supabase=supabase)

    # 2. Fetch course information
    course_res = (
        await supabase.table(COURSES_TABLE)
        .select("*")
        .eq("id", course_id)
        .limit(1)
        .execute()
    )
    if not course_res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found.",
        )
    course = course_res.data[0]
    course_title = course["title"]
    course_desc = course.get("description", "")
    difficulty = course.get("difficulty", "Intermediate")

    # 3. Fetch student profile context
    profile_res = (
        await supabase.table(STUDENT_PROFILES_TABLE)
        .select("*")
        .eq("user_id", user_id)
        .limit(1)
        .execute()
    )
    profile = profile_res.data[0] if profile_res.data else {}
    branch = profile.get("branch", "Engineering")

    # 4. Generate structured 4-phase path via Gemini
    prompt = f"""
You are an expert curriculum designer and university professor.
Design a rigorous, structured 4-phase learning path for the following undergraduate course:
Course Title: {course_title}
Description: {course_desc}
Difficulty: {difficulty}
Student Branch: {branch}

Return a valid JSON array of exactly 4 phases structured as follows:
[
  {{
    "phase_number": 1,
    "title": "Phase 1: Foundations & Fundamentals",
    "description": "Essential prerequisites and core underlying concepts.",
    "estimated_hours": 8.0,
    "topics": [
      {{
        "topic_number": 1,
        "title": "Introduction & Setup",
        "description": "Core concepts and environment configuration.",
        "resources": [
          {{
            "title": "Official Getting Started Guide",
            "url": "https://developer.mozilla.org/en-US/",
            "resource_type": "documentation",
            "estimated_minutes": 25
          }},
          {{
            "title": "Interactive Tutorial & Architecture Overview",
            "url": "https://www.freecodecamp.org/news/",
            "resource_type": "tutorial",
            "estimated_minutes": 40
          }}
        ]
      }},
      {{
        "topic_number": 2,
        "title": "Key Theoretical Concepts",
        "description": "Foundational building blocks and theory.",
        "resources": [
          {{
            "title": "Deep Dive Documentation",
            "url": "https://en.wikipedia.org/wiki/Computer_science",
            "resource_type": "article",
            "estimated_minutes": 30
          }}
        ]
      }}
    ]
  }},
  {{
    "phase_number": 2,
    "title": "Phase 2: Core Concepts & Deep Dive",
    "description": "In-depth practical principles and implementation patterns.",
    "estimated_hours": 12.0,
    "topics": [ ... ]
  }},
  {{
    "phase_number": 3,
    "title": "Phase 3: Practical Implementation & Advanced Concepts",
    "description": "Complex use cases, optimization, and scalable design.",
    "estimated_hours": 14.0,
    "topics": [ ... ]
  }},
  {{
    "phase_number": 4,
    "title": "Phase 4: Capstone Mastery & Real-World Projects",
    "description": "End-to-end industry project, assessment preparation, and deployment.",
    "estimated_hours": 16.0,
    "topics": [ ... ]
  }}
]
Ensure each phase contains between 2 and 4 high-quality topics, each with 1-3 curated resources.
"""

    phases_data: list[dict[str, Any]] = []
    try:
        raw_json = await call_gemini_with_retry(prompt)
        if raw_json:
            parsed = json.loads(raw_json)
            if isinstance(parsed, list):
                phases_data = parsed
            elif isinstance(parsed, dict) and "phases" in parsed:
                phases_data = parsed["phases"]
    except Exception as e:
        logger.warning("Gemini learning path generation failed: %s. Using default syllabus.", e)
        phases_data = [
            {
                "phase_number": 1,
                "title": "Phase 1: Foundations & Fundamentals",
                "description": f"Core prerequisites and fundamental principles of {course_title}.",
                "estimated_hours": 8.0,
                "topics": [
                    {
                        "topic_number": 1,
                        "title": "Course Overview & Foundational Concepts",
                        "description": "Key definitions, environment setup, and theoretical background.",
                        "resources": [
                            {"title": "Comprehensive Guide", "url": "https://docs.python.org/3/", "resource_type": "documentation", "estimated_minutes": 30},
                        ],
                    },
                    {
                        "topic_number": 2,
                        "title": "Basic Mechanics & Syntax",
                        "description": "Working with foundational building blocks and syntax.",
                        "resources": [
                            {"title": "Interactive Tutorial", "url": "https://www.w3schools.com/", "resource_type": "tutorial", "estimated_minutes": 35},
                        ],
                    },
                ],
            },
            {
                "phase_number": 2,
                "title": "Phase 2: Core Architecture & Patterns",
                "description": "Core algorithmic methods, state management, and structural design.",
                "estimated_hours": 10.0,
                "topics": [
                    {
                        "topic_number": 1,
                        "title": "Data Flow & Control Structures",
                        "description": "Designing efficient operational flows.",
                        "resources": [
                            {"title": "Architecture Overview", "url": "https://developer.mozilla.org/", "resource_type": "article", "estimated_minutes": 25},
                        ],
                    },
                ],
            },
            {
                "phase_number": 3,
                "title": "Phase 3: Advanced Optimization & Scaling",
                "description": "Performance tuning, error handling, and robust production designs.",
                "estimated_hours": 12.0,
                "topics": [
                    {
                        "topic_number": 1,
                        "title": "Scaling & Fault Tolerance",
                        "description": "Handling edge cases and optimizing execution speed.",
                        "resources": [
                            {"title": "Best Practices Guide", "url": "https://github.com/trending", "resource_type": "article", "estimated_minutes": 30},
                        ],
                    },
                ],
            },
            {
                "phase_number": 4,
                "title": "Phase 4: Capstone Project & Evaluation",
                "description": "End-to-end project realization, code review, and final assessment.",
                "estimated_hours": 14.0,
                "topics": [
                    {
                        "topic_number": 1,
                        "title": "Capstone Project Delivery",
                        "description": "Final hands-on implementation and presentation.",
                        "resources": [
                            {"title": "Project Specification & Checklist", "url": "https://github.com", "resource_type": "tutorial", "estimated_minutes": 45},
                        ],
                    },
                ],
            },
        ]

    # 5. Persist phases, topics, and resources into Supabase
    for phase_info in phases_data:
        phase_insert = {
            "course_id": course_id,
            "phase_number": phase_info.get("phase_number", 1),
            "title": phase_info.get("title", "Phase"),
            "description": phase_info.get("description", ""),
            "estimated_hours": phase_info.get("estimated_hours", 10.0),
        }
        phase_res = await supabase.table(LEARNING_PHASES_TABLE).insert(phase_insert).execute()
        if not phase_res.data:
            continue
        phase_record = phase_res.data[0]
        phase_id = phase_record["id"]

        for topic_info in phase_info.get("topics", []):
            topic_insert = {
                "phase_id": phase_id,
                "topic_number": topic_info.get("topic_number", 1),
                "title": topic_info.get("title", "Topic"),
                "description": topic_info.get("description", ""),
            }
            topic_res = await supabase.table(PHASE_TOPICS_TABLE).insert(topic_insert).execute()
            if not topic_res.data:
                continue
            topic_record = topic_res.data[0]
            topic_id = topic_record["id"]

            for res_info in topic_info.get("resources", []):
                res_insert = {
                    "topic_id": topic_id,
                    "title": res_info.get("title", "Resource"),
                    "url": res_info.get("url", "https://google.com"),
                    "resource_type": res_info.get("resource_type", "article"),
                    "estimated_minutes": res_info.get("estimated_minutes", 20),
                }
                await supabase.table(RESOURCES_TABLE).insert(res_insert).execute()

    return await get_learning_path(course_id=course_id, user_id=user_id, supabase=supabase)


async def get_learning_path(
    course_id: str,
    user_id: str,
    supabase: AsyncClient,
) -> LearningPath:
    """Retrieve the complete learning path with completion annotations for the student.

    Args:
        course_id: UUID of course.
        user_id: UUID of student.
        supabase: Active Supabase AsyncClient.

    Returns:
        LearningPath model.
    """
    # Course title
    course_res = await supabase.table(COURSES_TABLE).select("title").eq("id", course_id).limit(1).execute()
    course_title = course_res.data[0]["title"] if course_res.data else "Course"

    # Fetch phases
    phases_res = (
        await supabase.table(LEARNING_PHASES_TABLE)
        .select("*")
        .eq("course_id", course_id)
        .order("phase_number")
        .execute()
    )
    raw_phases = phases_res.data or []

    # Completed topic IDs for this student
    perf_res = (
        await supabase.table(TOPIC_PERFORMANCE_TABLE)
        .select("topic_id")
        .eq("user_id", user_id)
        .eq("is_completed", True)
        .execute()
    )
    completed_ids = {str(item["topic_id"]) for item in (perf_res.data or [])}

    phases_models: list[LearningPhase] = []
    total_topics = 0
    completed_topics_count = 0

    for phase in raw_phases:
        phase_id = phase["id"]
        topics_res = (
            await supabase.table(PHASE_TOPICS_TABLE)
            .select("*, resources(*)")
            .eq("phase_id", phase_id)
            .order("topic_number")
            .execute()
        )
        topics_list: list[PhaseTopic] = []
        for t in (topics_res.data or []):
            t_id = str(t["id"])
            is_comp = t_id in completed_ids
            total_topics += 1
            if is_comp:
                completed_topics_count += 1

            resources = [Resource.model_validate(r) for r in (t.get("resources") or [])]
            topics_list.append(
                PhaseTopic(
                    id=t_id,
                    phase_id=phase_id,
                    topic_number=t.get("topic_number", 1),
                    title=t.get("title", ""),
                    description=t.get("description"),
                    resources=resources,
                    is_completed=is_comp,
                )
            )

        phases_models.append(
            LearningPhase(
                id=phase_id,
                course_id=course_id,
                phase_number=phase.get("phase_number", 1),
                title=phase.get("title", ""),
                description=phase.get("description"),
                estimated_hours=float(phase.get("estimated_hours") or 10.0),
                topics=topics_list,
            )
        )

    overall_progress = (
        round((completed_topics_count / total_topics) * 100.0, 1)
        if total_topics > 0
        else 0.0
    )

    return LearningPath(
        course_id=course_id,
        course_title=course_title,
        phases=phases_models,
        overall_progress=overall_progress,
    )


async def get_phase_detail(
    phase_id: str,
    user_id: str,
    supabase: AsyncClient,
) -> LearningPhase:
    """Retrieve full details for a single learning phase.

    Args:
        phase_id: UUID of phase.
        user_id: UUID of student.
        supabase: Active Supabase AsyncClient.

    Returns:
        LearningPhase model.
    """
    phase_res = (
        await supabase.table(LEARNING_PHASES_TABLE)
        .select("*")
        .eq("id", phase_id)
        .limit(1)
        .execute()
    )
    if not phase_res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Learning phase not found.",
        )
    phase = phase_res.data[0]

    # Completed topic IDs for this student
    perf_res = (
        await supabase.table(TOPIC_PERFORMANCE_TABLE)
        .select("topic_id")
        .eq("user_id", user_id)
        .eq("is_completed", True)
        .execute()
    )
    completed_ids = {str(item["topic_id"]) for item in (perf_res.data or [])}

    topics_res = (
        await supabase.table(PHASE_TOPICS_TABLE)
        .select("*, resources(*)")
        .eq("phase_id", phase_id)
        .order("topic_number")
        .execute()
    )
    topics_list: list[PhaseTopic] = []
    for t in (topics_res.data or []):
        t_id = str(t["id"])
        resources = [Resource.model_validate(r) for r in (t.get("resources") or [])]
        topics_list.append(
            PhaseTopic(
                id=t_id,
                phase_id=phase_id,
                topic_number=t.get("topic_number", 1),
                title=t.get("title", ""),
                description=t.get("description"),
                resources=resources,
                is_completed=t_id in completed_ids,
            )
        )

    return LearningPhase(
        id=phase_id,
        course_id=phase["course_id"],
        phase_number=phase.get("phase_number", 1),
        title=phase.get("title", ""),
        description=phase.get("description"),
        estimated_hours=float(phase.get("estimated_hours") or 10.0),
        topics=topics_list,
    )


async def complete_topic(
    user_id: str,
    topic_id: str,
    supabase: AsyncClient,
) -> dict[str, Any]:
    """Mark a topic as completed and update the course enrollment progress percentage.

    Args:
        user_id: UUID of student.
        topic_id: UUID of topic.
        supabase: Active Supabase AsyncClient.

    Returns:
        Status message dict.
    """
    # 1. Fetch topic & course relation
    topic_res = (
        await supabase.table(PHASE_TOPICS_TABLE)
        .select("id, phase_id, learning_phases(course_id)")
        .eq("id", topic_id)
        .limit(1)
        .execute()
    )
    if not topic_res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Topic not found.",
        )
    topic_data = topic_res.data[0]
    course_id = (topic_data.get("learning_phases") or {}).get("course_id")

    # 2. Upsert topic_performance record
    existing = (
        await supabase.table(TOPIC_PERFORMANCE_TABLE)
        .select("id")
        .eq("user_id", user_id)
        .eq("topic_id", topic_id)
        .limit(1)
        .execute()
    )
    now_iso = datetime.now(timezone.utc).isoformat()
    if existing.data:
        await (
            supabase.table(TOPIC_PERFORMANCE_TABLE)
            .update({"is_completed": True, "updated_at": now_iso})
            .eq("id", existing.data[0]["id"])
            .execute()
        )
    else:
        await (
            supabase.table(TOPIC_PERFORMANCE_TABLE)
            .insert({
                "user_id": user_id,
                "topic_id": topic_id,
                "is_completed": True,
                "mastery_level": "mastered",
                "updated_at": now_iso,
            })
            .execute()
        )

    # 3. Recalculate enrollment progress if course_id known
    if course_id:
        path = await get_learning_path(course_id=course_id, user_id=user_id, supabase=supabase)
        status_val = "completed" if path.overall_progress >= 100.0 else "in_progress"
        await (
            supabase.table(COURSE_ENROLLMENTS_TABLE)
            .update({
                "progress_percentage": path.overall_progress,
                "status": status_val,
            })
            .eq("user_id", user_id)
            .eq("course_id", course_id)
            .execute()
        )

    return {"message": "Topic marked as complete.", "success": True}


async def get_phase_progress(
    user_id: str,
    phase_id: str,
    supabase: AsyncClient,
) -> PhaseProgress:
    """Calculate topic completion count and percentage for a learning phase.

    Args:
        user_id: UUID of student.
        phase_id: UUID of phase.
        supabase: Active Supabase AsyncClient.

    Returns:
        PhaseProgress model.
    """
    phase_res = (
        await supabase.table(LEARNING_PHASES_TABLE)
        .select("id, phase_number, title")
        .eq("id", phase_id)
        .limit(1)
        .execute()
    )
    if not phase_res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Learning phase not found.",
        )
    p = phase_res.data[0]

    topics_res = await supabase.table(PHASE_TOPICS_TABLE).select("id").eq("phase_id", phase_id).execute()
    topic_ids = [str(t["id"]) for t in (topics_res.data or [])]
    total_topics = len(topic_ids)

    if total_topics == 0:
        return PhaseProgress(
            phase_id=phase_id,
            phase_number=p.get("phase_number", 1),
            phase_title=p.get("title", ""),
            total_topics=0,
            completed_topics=0,
            percentage=0.0,
            is_completed=False,
        )

    perf_res = (
        await supabase.table(TOPIC_PERFORMANCE_TABLE)
        .select("topic_id")
        .eq("user_id", user_id)
        .eq("is_completed", True)
        .in_("topic_id", topic_ids)
        .execute()
    )
    completed_topics = len(perf_res.data or [])
    percentage = round((completed_topics / total_topics) * 100.0, 1)

    return PhaseProgress(
        phase_id=phase_id,
        phase_number=p.get("phase_number", 1),
        phase_title=p.get("title", ""),
        total_topics=total_topics,
        completed_topics=completed_topics,
        percentage=percentage,
        is_completed=percentage >= 100.0,
    )
