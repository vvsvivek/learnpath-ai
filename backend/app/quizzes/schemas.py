"""
Quiz, Question, Submission, and Assessment Result Pydantic v2 Schemas.
"""

from datetime import datetime
from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class QuizQuestionStudent(BaseModel):
    """Question schema for active quiz taking (answers hidden)."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    question_text: str
    options: list[str]


class QuizQuestion(BaseModel):
    """Full question schema including correct option and explanation."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    quiz_id: str
    question_text: str
    options: list[str]
    correct_option: int
    explanation: str | None = None


class Quiz(BaseModel):
    """Full Quiz schema."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    phase_id: str
    title: str
    description: str | None = None
    time_limit_minutes: int = 15
    questions: list[QuizQuestion] = Field(default_factory=list)


class QuizStudentView(BaseModel):
    """Quiz schema displayed to student when taking the quiz."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    phase_id: str
    title: str
    description: str | None = None
    time_limit_minutes: int = 15
    total_questions: int = 0
    questions: list[QuizQuestionStudent] = Field(default_factory=list)


class QuizSubmission(BaseModel):
    """Submission payload mapping question ID to selected option index."""
    model_config = ConfigDict(str_strip_whitespace=True)

    answers: dict[str, int] = Field(
        ...,
        description="Map of question_id UUID to chosen integer option index (0, 1, 2, or 3)",
    )


class QuestionFeedback(BaseModel):
    """Detailed evaluation of an individual submitted answer."""
    question_id: str
    question_text: str
    selected_option: int | None
    correct_option: int
    is_correct: bool
    explanation: str | None = None


class QuizResult(BaseModel):
    """Result and feedback report returned upon quiz completion."""
    attempt_id: str
    quiz_id: str
    score: int
    total_questions: int
    percentage: float
    passed: bool
    feedback: str
    detailed_answers: list[QuestionFeedback] = Field(default_factory=list)


class QuizAttempt(BaseModel):
    """Quiz attempt history entity."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    user_id: str
    quiz_id: str
    score: int
    total_questions: int
    percentage: float
    passed: bool = False
    completed_at: datetime | str | None = None
    quiz: dict[str, Any] | None = None
