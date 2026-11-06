"""
Chatbot API Router for Student Tutoring and Faculty Institutional Analytics.
"""

from typing import Any
from fastapi import APIRouter, Depends, status
from pydantic import BaseModel, ConfigDict, Field
from supabase import AsyncClient
from app.chatbot.faculty_ai import faculty_chat_interaction
from app.chatbot.student_ai import student_chat_interaction
from app.core.dependencies import get_current_user, get_supabase, require_faculty, require_student
from app.database.models import (
    CHAT_CONVERSATIONS_TABLE,
    CHAT_MESSAGES_TABLE,
)

router = APIRouter(prefix="/chat", tags=["AI Chatbot"])


class ChatMessageRequest(BaseModel):
    """Schema for incoming user chat message."""
    model_config = ConfigDict(str_strip_whitespace=True)

    message: str = Field(..., min_length=1, max_length=4000)
    conversation_id: str | None = None


class CreateConversationRequest(BaseModel):
    """Schema for initializing a new chat conversation."""
    model_config = ConfigDict(str_strip_whitespace=True)

    title: str | None = Field(default="New Conversation", max_length=200)


@router.post(
    "/student/message",
    response_model=dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Send message to Student AI Tutor",
)
async def student_message_endpoint(
    payload: ChatMessageRequest,
    current_user: dict[str, Any] = Depends(require_student),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """Ask question or request mentoring assistance from personalized Student AI Tutor."""
    user_id = str(current_user["id"])
    return await student_chat_interaction(
        user_id=user_id,
        message=payload.message,
        conversation_id=payload.conversation_id,
        supabase=supabase,
    )


@router.post(
    "/faculty/message",
    response_model=dict[str, Any],
    status_code=status.HTTP_200_OK,
    summary="Send message to Faculty AI Advisor",
)
async def faculty_message_endpoint(
    payload: ChatMessageRequest,
    current_user: dict[str, Any] = Depends(require_faculty),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """Query departmental institutional performance metrics and curriculum recommendations."""
    user_id = str(current_user["id"])
    return await faculty_chat_interaction(
        user_id=user_id,
        message=payload.message,
        conversation_id=payload.conversation_id,
        supabase=supabase,
    )


@router.post(
    "/conversations",
    response_model=dict[str, Any],
    status_code=status.HTTP_201_CREATED,
    summary="Create a new chat conversation thread",
)
async def create_conversation_endpoint(
    payload: CreateConversationRequest,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> dict[str, Any]:
    """Spawn an empty chat thread for the authenticated user."""
    user_id = str(current_user["id"])
    role = current_user.get("role", "student")
    res = (
        await supabase.table(CHAT_CONVERSATIONS_TABLE)
        .insert({
            "user_id": user_id,
            "role": role,
            "title": payload.title or "New Conversation",
        })
        .execute()
    )
    return res.data[0]


@router.get(
    "/conversations",
    response_model=list[dict[str, Any]],
    status_code=status.HTTP_200_OK,
    summary="List all user conversation threads",
)
async def list_conversations_endpoint(
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> list[dict[str, Any]]:
    """Retrieve user's chat conversations ordered by recency."""
    user_id = str(current_user["id"])
    res = (
        await supabase.table(CHAT_CONVERSATIONS_TABLE)
        .select("*")
        .eq("user_id", user_id)
        .order("updated_at", desc=True)
        .execute()
    )
    return res.data or []


@router.get(
    "/conversations/{conversation_id}/messages",
    response_model=list[dict[str, Any]],
    status_code=status.HTTP_200_OK,
    summary="Get all messages in a conversation thread",
)
async def get_conversation_messages_endpoint(
    conversation_id: str,
    current_user: dict[str, Any] = Depends(get_current_user),
    supabase: AsyncClient = Depends(get_supabase),
) -> list[dict[str, Any]]:
    """Fetch chronological message exchange history for a thread."""
    user_id = str(current_user["id"])
    # Verify ownership
    conv_check = (
        await supabase.table(CHAT_CONVERSATIONS_TABLE)
        .select("id")
        .eq("id", conversation_id)
        .eq("user_id", user_id)
        .limit(1)
        .execute()
    )
    if not conv_check.data:
        return []

    res = (
        await supabase.table(CHAT_MESSAGES_TABLE)
        .select("*")
        .eq("conversation_id", conversation_id)
        .order("created_at", desc=False)
        .execute()
    )
    return res.data or []
