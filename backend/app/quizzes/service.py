"""
Quiz Generation via Gemini, Grading, and Performance Tracking Service.
"""

from datetime import datetime, timezone
import json
import logging
from typing import Any
from fastapi import HTTPException, status
from supabase import AsyncClient
from app.courses.service import call_gemini_with_retry
from app.database.models import (
    LEARNING_PHASES_TABLE,
    PHASE_TOPICS_TABLE,
    QUIZ_ATTEMPTS_TABLE,
    QUIZ_QUESTIONS_TABLE,
    QUIZZES_TABLE,
    TOPIC_PERFORMANCE_TABLE,
)
from app.quizzes.schemas import (
    QuestionFeedback,
    Quiz,
    QuizAttempt,
    QuizQuestion,
    QuizQuestionStudent,
    QuizResult,
    QuizStudentView,
)

logger = logging.getLogger(__name__)


async def generate_quiz_for_phase(
    phase_id: str,
    supabase: AsyncClient,
) -> QuizStudentView:
    """Generate or retrieve a multi-question MCQ quiz for a specific learning phase.

    Args:
        phase_id: UUID of phase.
        supabase: Active Supabase AsyncClient.

    Returns:
        QuizStudentView with questions.
    """
    # 1. Check if quiz already exists for this phase
    existing_quiz = (
        await supabase.table(QUIZZES_TABLE)
        .select("*")
        .eq("phase_id", phase_id)
        .limit(1)
        .execute()
    )
    if existing_quiz.data:
        return await get_quiz_student_view(phase_id=phase_id, supabase=supabase)

    # 2. Fetch phase and topics context
    phase_res = (
        await supabase.table(LEARNING_PHASES_TABLE)
        .select("*, courses(title)")
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
    phase_title = phase["title"]
    course_title = (phase.get("courses") or {}).get("title") or "Engineering Course"

    topics_res = await supabase.table(PHASE_TOPICS_TABLE).select("title, description").eq("phase_id", phase_id).execute()
    topic_titles = [t["title"] for t in (topics_res.data or [])]

    # 3. Request Gemini to generate 5 rigorous MCQs
    prompt = f"""
You are an engineering examination evaluator.
Create a 5-question multiple choice assessment for students who just finished:
Course: {course_title}
Phase: {phase_title}
Key Topics Covered: {', '.join(topic_titles)}

Return a valid JSON array of exactly 5 questions with this exact structure:
[
  {{
    "question_text": "What is the primary trade-off when implementing...?",
    "options": [
      "Option A explanation",
      "Option B explanation",
      "Option C explanation",
      "Option D explanation"
    ],
    "correct_option": 0,
    "explanation": "Option A is correct because..."
  }}
]
Note: correct_option must be an integer index from 0 to 3.
"""

    questions_data: list[dict[str, Any]] = []
    try:
        raw_json = await call_gemini_with_retry(prompt)
        if raw_json:
            parsed = json.loads(raw_json)
            if isinstance(parsed, list):
                questions_data = parsed
            elif isinstance(parsed, dict) and "questions" in parsed:
                questions_data = parsed["questions"]
    except Exception as e:
        logger.warning("Gemini quiz generation failed: %s. Using default assessment.", e)
        questions_data = [
            {
                "question_text": f"What is the foundational principle underlying {phase_title}?",
                "options": [
                    "Separation of concerns and modular abstraction",
                    "Monolithic code duplication",
                    "Ignoring edge cases and error bounds",
                    "Hardcoding configuration parameters",
                ],
                "correct_option": 0,
                "explanation": "Separation of concerns ensures robust, maintainable architecture.",
            },
            {
                "question_text": "Which of the following practices most effectively optimizes execution performance?",
                "options": [
                    "Continuous busy-waiting polling",
                    "Efficient asymptotic algorithmic complexity and caching",
                    "Increasing network round trips",
                    "Disabling compilation optimizations",
                ],
                "correct_option": 1,
                "explanation": "Algorithmic complexity and intelligent caching directly minimize resource consumption.",
            },
            {
                "question_text": "How should unexpected runtime exceptions be addressed in scalable systems?",
                "options": [
                    "Silently catching and suppressing all exceptions",
                    "Crashing the server process immediately",
                    "Structured logging, error boundaries, and defensive retry policies",
                    "Printing raw stack traces to end users",
                ],
                "correct_option": 2,
                "explanation": "Structured error handling and resilience patterns maintain system availability.",
            },
        ]

    # 4. Insert quiz into quizzes table
    quiz_insert = {
        "phase_id": phase_id,
        "title": f"Phase Assessment: {phase_title}",
        "description": f"Knowledge evaluation test covering {phase_title}.",
        "time_limit_minutes": 15,
    }
    quiz_res = await supabase.table(QUIZZES_TABLE).insert(quiz_insert).execute()
    if not quiz_res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to record quiz.",
        )
    quiz_record = quiz_res.data[0]
    quiz_id = quiz_record["id"]

    # 5. Insert questions
    for q in questions_data:
        q_insert = {
            "quiz_id": quiz_id,
            "question_text": q.get("question_text", "Question"),
            "options": q.get("options", ["A", "B", "C", "D"]),
            "correct_option": int(q.get("correct_option", 0)),
            "explanation": q.get("explanation", ""),
        }
        await supabase.table(QUIZ_QUESTIONS_TABLE).insert(q_insert).execute()

    return await get_quiz_student_view(phase_id=phase_id, supabase=supabase)


async def get_quiz_student_view(
    phase_id: str,
    supabase: AsyncClient,
) -> QuizStudentView:
    """Retrieve quiz questions formatted for student participation (omitting answer keys).

    Args:
        phase_id: UUID of phase.
        supabase: Active Supabase AsyncClient.

    Returns:
        QuizStudentView model.
    """
    quiz_res = (
        await supabase.table(QUIZZES_TABLE)
        .select("*")
        .eq("phase_id", phase_id)
        .limit(1)
        .execute()
    )
    if not quiz_res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No quiz found for this learning phase. Please generate one first.",
        )
    quiz = quiz_res.data[0]
    quiz_id = quiz["id"]

    questions_res = (
        await supabase.table(QUIZ_QUESTIONS_TABLE)
        .select("id, question_text, options")
        .eq("quiz_id", quiz_id)
        .execute()
    )
    student_questions = [
        QuizQuestionStudent(
            id=str(q["id"]),
            question_text=q["question_text"],
            options=q["options"] if isinstance(q["options"], list) else json.loads(q["options"]),
        )
        for q in (questions_res.data or [])
    ]

    return QuizStudentView(
        id=str(quiz_id),
        phase_id=phase_id,
        title=quiz["title"],
        description=quiz.get("description"),
        time_limit_minutes=quiz.get("time_limit_minutes", 15),
        total_questions=len(student_questions),
        questions=student_questions,
    )


async def submit_quiz_answers(
    user_id: str,
    quiz_id: str,
    answers: dict[str, int],
    supabase: AsyncClient,
) -> QuizResult:
    """Grade submitted answers, persist attempt record, and update topic performance.

    Args:
        user_id: UUID of student.
        quiz_id: UUID of quiz.
        answers: Map of question_id -> chosen option index (0-3).
        supabase: Active Supabase AsyncClient.

    Returns:
        QuizResult with detailed feedback and score.
    """
    # Fetch questions
    questions_res = (
        await supabase.table(QUIZ_QUESTIONS_TABLE)
        .select("*")
        .eq("quiz_id", quiz_id)
        .execute()
    )
    questions = questions_res.data or []
    if not questions:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No questions found for this quiz.",
        )

    score = 0
    total = len(questions)
    detailed_answers: list[QuestionFeedback] = []

    for q in questions:
        q_id = str(q["id"])
        correct_opt = int(q["correct_option"])
        selected_opt = answers.get(q_id)
        is_corr = (selected_opt == correct_opt)
        if is_corr:
            score += 1

        detailed_answers.append(
            QuestionFeedback(
                question_id=q_id,
                question_text=q["question_text"],
                selected_option=selected_opt,
                correct_option=correct_opt,
                is_correct=is_corr,
                explanation=q.get("explanation"),
            )
        )

    percentage = round((score / total) * 100.0, 1)
    passed = percentage >= 60.0

    # Persist attempt
    attempt_payload = {
        "user_id": user_id,
        "quiz_id": quiz_id,
        "score": score,
        "total_questions": total,
        "percentage": percentage,
        "passed": passed,
        "answers": answers,
        "completed_at": datetime.now(timezone.utc).isoformat(),
    }
    attempt_res = await supabase.table(QUIZ_ATTEMPTS_TABLE).insert(attempt_payload).execute()
    if not attempt_res.data:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to record quiz attempt.",
        )
    attempt_id = attempt_res.data[0]["id"]

    # Update associated phase topics' performance
    quiz_meta = await supabase.table(QUIZZES_TABLE).select("phase_id").eq("id", quiz_id).limit(1).execute()
    if quiz_meta.data:
        phase_id = quiz_meta.data[0]["phase_id"]
        topics = await supabase.table(PHASE_TOPICS_TABLE).select("id").eq("phase_id", phase_id).execute()
        mastery = "mastered" if passed else "needs_practice"
        for t in (topics.data or []):
            t_id = t["id"]
            existing = (
                await supabase.table(TOPIC_PERFORMANCE_TABLE)
                .select("id")
                .eq("user_id", user_id)
                .eq("topic_id", t_id)
                .limit(1)
                .execute()
            )
            now_iso = datetime.now(timezone.utc).isoformat()
            if existing.data:
                await (
                    supabase.table(TOPIC_PERFORMANCE_TABLE)
                    .update({
                        "last_quiz_score": percentage,
                        "mastery_level": mastery,
                        "updated_at": now_iso,
                    })
                    .eq("id", existing.data[0]["id"])
                    .execute()
                )
            else:
                await (
                    supabase.table(TOPIC_PERFORMANCE_TABLE)
                    .insert({
                        "user_id": user_id,
                        "topic_id": t_id,
                        "is_completed": False,
                        "mastery_level": mastery,
                        "last_quiz_score": percentage,
                        "updated_at": now_iso,
                    })
                    .execute()
                )

    feedback_text = (
        f"Outstanding work! You demonstrated strong mastery of this phase with {percentage}%."
        if percentage >= 80.0
        else f"Good effort! You passed with {percentage}%. Review the explanations below to refine weaker areas."
        if passed
        else f"You scored {percentage}%. We recommend reviewing the curated resources and retaking this assessment."
    )

    return QuizResult(
        attempt_id=str(attempt_id),
        quiz_id=quiz_id,
        score=score,
        total_questions=total,
        percentage=percentage,
        passed=passed,
        feedback=feedback_text,
        detailed_answers=detailed_answers,
    )


async def get_student_quiz_history(
    user_id: str,
    supabase: AsyncClient,
) -> list[QuizAttempt]:
    """Retrieve all past quiz attempts for a student.

    Args:
        user_id: UUID of student.
        supabase: Active Supabase AsyncClient.

    Returns:
        List of QuizAttempt models.
    """
    res = (
        await supabase.table(QUIZ_ATTEMPTS_TABLE)
        .select("*, quizzes(title, phase_id)")
        .eq("user_id", user_id)
        .order("completed_at", desc=True)
        .execute()
    )
    return [QuizAttempt.model_validate(row) for row in (res.data or [])]


async def get_attempt_detail(
    attempt_id: str,
    supabase: AsyncClient,
) -> dict[str, Any]:
    """Retrieve full details of a specific quiz attempt.

    Args:
        attempt_id: UUID of attempt.
        supabase: Active Supabase AsyncClient.

    Returns:
        Dict of attempt record with quiz metadata.
    """
    res = (
        await supabase.table(QUIZ_ATTEMPTS_TABLE)
        .select("*, quizzes(*)")
        .eq("id", attempt_id)
        .limit(1)
        .execute()
    )
    if not res.data:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Quiz attempt not found.",
        )
    return res.data[0]
