"""
Quizzes, Assessments, and Exam History API Router.
"""

from typing import Any
from fastapi import APIRouter, Depends, status
from supabase import AsyncClient
from app.core.dependencies import get_current_user, get_supabase, require_student
from app.quizzes.schemas import (
    QuizAttempt,
    QuizResult,
    QuizStudentView,
    QuizSubmission,
)
from app.quizzes.service import (
    generate_quiz_for_phase,
    get_attempt_detail,
    get_quiz_student_view,
    get_student_quiz_history,
    submit_quiz_answers,
)

router = APIRouter(prefix="/quizzes", tags=["Quizzes"])


@router.post(
    "/phases/{phase_id}/generate",
    response_model=QuizStudentView,
    status_code=status.HTTP_201_CREATED,
    summary="Generate assessment questions for a phase",
)
async def generate_quiz_endpoint(
    phase_id: str,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> QuizStudentView:
    """Generate or retrieve AI-curated multiple choice evaluation questions."""
    return await generate_quiz_for_phase(phase_id=phase_id, supabase=supabase)


@router.get(
    "/phases/{phase_id}",
    response_model=QuizStudentView,
    status_code=status.HTTP_200_OK,
    summary="Get active quiz questions for student evaluation",
)
async def get_quiz_endpoint(
    phase_id: str,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> QuizStudentView:
    """Retrieve quiz questions formatted for student submission without answer reveals."""
    return await get_quiz_student_view(phase_id=phase_id, supabase=supabase)


@router.post(
    "/{quiz_id}/submit",
    response_model=QuizResult,
    status_code=status.HTTP_200_OK,
    summary="Submit answers and receive real-time grading and feedback",
)
async def submit_quiz_endpoint(
    quiz_id: str,
    payload: QuizSubmission,
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> QuizResult:
    """Grade submitted question responses, store score, and return explanations."""
    user_id = str(current_user["id"])
    return await submit_quiz_answers(
        user_id=user_id,
        quiz_id=quiz_id,
        answers=payload.answers,
        supabase=supabase,
    )


@router.get(
    "/history",
    response_model=list[QuizAttempt],
    status_code=status.HTTP_200_OK,
    summary="List student's historical quiz evaluations",
)
async def get_history_endpoint(
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> list[QuizAttempt]:
    """Retrieve full chronological list of past quiz attempts and scores."""
    user_id = str(current_user["id"])
    return await get_student_quiz_history(user_id=user_id, supabase=supabase)


@router.get(
    "/attempts/{attempt_id}",
    response_model=dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Inspect individual quiz attempt details",
)
async def get_attempt_endpoint(
    attempt_id: str,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """View saved answers, score, and timestamp for a specific attempt."""
    return await get_attempt_detail(attempt_id=attempt_id, supabase=supabase)
