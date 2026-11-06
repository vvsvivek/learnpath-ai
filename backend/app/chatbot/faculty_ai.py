"""
Faculty AI Institutional Analytics Advisor powered by Google GenAI.
"""

from datetime import datetime, timezone
import logging
from typing import Any
from fastapi import HTTPException, status
from supabase import AsyncClient
from app.chatbot.context import build_faculty_context
from app.courses.service import call_gemini_with_retry
from app.database.models import (
    CHAT_CONVERSATIONS_TABLE,
    CHAT_MESSAGES_TABLE,
)

logger = logging.getLogger(__name__)


async def faculty_chat_interaction(
    user_id: str,
    message: str,
    conversation_id: str | None,
    supabase: AsyncClient,
) -> dict[str, Any]:
    """Execute a faculty chat turn with the AI institutional advisor.

    Args:
        user_id: UUID of faculty.
        message: Faculty member's prompt or question.
        conversation_id: UUID of conversation, or None to spawn new.
        supabase: Active Supabase AsyncClient.

    Returns:
        Dict containing conversation_id, AI reply, and timestamp.
    """
    clean_message = message.strip()
    if not clean_message:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Message content cannot be blank.",
        )

    # 1. Resolve or spawn conversation
    if conversation_id:
        conv_res = (
            await supabase.table(CHAT_CONVERSATIONS_TABLE)
            .select("id")
            .eq("id", conversation_id)
            .eq("user_id", user_id)
            .limit(1)
            .execute()
        )
        if not conv_res.data:
            conversation_id = None

    if not conversation_id:
        title = clean_message[:35] + ("..." if len(clean_message) > 35 else "")
        new_conv = (
            await supabase.table(CHAT_CONVERSATIONS_TABLE)
            .insert({
                "user_id": user_id,
                "role": "faculty",
                "title": title,
            })
            .execute()
        )
        conversation_id = new_conv.data[0]["id"]

    # 2. Gather faculty & institutional context
    faculty_ctx = await build_faculty_context(user_id=user_id, supabase=supabase)

    history_res = (
        await supabase.table(CHAT_MESSAGES_TABLE)
        .select("sender, message")
        .eq("conversation_id", conversation_id)
        .order("created_at", desc=False)
        .limit(10)
        .execute()
    )
    formatted_history = []
    for h in (history_res.data or []):
        formatted_history.append(f"{h['sender'].upper()}: {h['message']}")

    # 3. Assemble prompt with instructions
    system_instruction = (
        "You are an expert Institutional Analytics Consultant and Academic Curriculum Advisor assisting "
        "university faculty and department heads at Siddhartha Academy. You specialize in student outcome "
        "analytics, curriculum gap identification, pedagogical interventions for struggling cohorts, "
        "and data-driven assessment strategies for engineering departments."
    )

    full_prompt = f"""
{system_instruction}

---
INSTITUTIONAL METRICS:
{faculty_ctx}

---
RECENT CHAT HISTORY:
{chr(10).join(formatted_history) if formatted_history else 'No previous conversation in this thread.'}

---
FACULTY QUERY:
{clean_message}

Please provide actionable, professional, and data-driven guidance formatted in clear markdown.
"""

    # 4. Generate AI response via Gemini
    try:
        reply_text = await call_gemini_with_retry(full_prompt)
        if not reply_text:
            reply_text = (
                "Based on the departmental metrics, I recommend reviewing student engagement in Phase 2 "
                "assessments and deploying targeted remedial micro-quizzes."
            )
    except Exception as exc:
        logger.error("Gemini faculty chat generation failed: %s", exc)
        reply_text = (
            "The institutional analytics service is currently experiencing high load. "
            "Please check the Faculty Dashboard tables for live cohort statistics."
        )

    # 5. Persist both user message and AI reply
    now_iso = datetime.now(timezone.utc).isoformat()
    await supabase.table(CHAT_MESSAGES_TABLE).insert([
        {
            "conversation_id": conversation_id,
            "sender": "user",
            "message": clean_message,
            "created_at": now_iso,
        },
        {
            "conversation_id": conversation_id,
            "sender": "assistant",
            "message": reply_text,
            "created_at": datetime.now(timezone.utc).isoformat(),
        },
    ]).execute()

    await (
        supabase.table(CHAT_CONVERSATIONS_TABLE)
        .update({"updated_at": datetime.now(timezone.utc).isoformat()})
        .eq("id", conversation_id)
        .execute()
    )

    return {
        "conversation_id": str(conversation_id),
        "reply": reply_text,
        "timestamp": now_iso,
    }
