"""
Learning Paths, Phases, Topics, and Curated Resources Pydantic v2 Schemas.
"""

from datetime import datetime
from typing import Any
from pydantic import BaseModel, ConfigDict, Field


class Resource(BaseModel):
    """Learning resource schema (documentation, video, article)."""
    model_config = ConfigDict(from_attributes=True)

    id: str | None = None
    topic_id: str | None = None
    title: str
    url: str
    resource_type: str = "article"
    estimated_minutes: int = 20


class PhaseTopic(BaseModel):
    """Topic entity within a learning phase."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    phase_id: str
    topic_number: int
    title: str
    description: str | None = None
    resources: list[Resource] = Field(default_factory=list)
    is_completed: bool = False


class LearningPhase(BaseModel):
    """Learning phase entity (e.g. Phase 1: Foundations to Phase 4: Capstone)."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    course_id: str
    phase_number: int
    title: str
    description: str | None = None
    estimated_hours: float = 10.0
    topics: list[PhaseTopic] = Field(default_factory=list)


class LearningPath(BaseModel):
    """Complete structured learning path for a course."""
    model_config = ConfigDict(from_attributes=True)

    course_id: str
    course_title: str
    phases: list[LearningPhase] = Field(default_factory=list)
    overall_progress: float = 0.0


class PhaseProgress(BaseModel):
    """Progress statistics for an individual learning phase."""
    phase_id: str
    phase_number: int
    phase_title: str
    total_topics: int = 0
    completed_topics: int = 0
    percentage: float = 0.0
    is_completed: bool = False
