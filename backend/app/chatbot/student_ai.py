"""
Student AI Academic Tutor and Mentor powered by Google GenAI.
"""

from datetime import datetime, timezone
import logging
from typing import Any
from fastapi import HTTPException, status
from supabase import AsyncClient
from app.chatbot.context import build_student_context
from app.courses.service import call_gemini_with_retry
from app.database.models import (
    CHAT_CONVERSATIONS_TABLE,
    CHAT_MESSAGES_TABLE,
)

logger = logging.getLogger(__name__)


async def student_chat_interaction(
    user_id: str,
    message: str,
    conversation_id: str | None,
    supabase: AsyncClient,
) -> dict[str, Any]:
    """Execute a student chat turn with the AI tutor, incorporating student context and chat history.

    Args:
        user_id: UUID of student.
        message: Student's query or message.
        conversation_id: UUID of conversation, or None to spawn new conversation.
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

    # 1. Resolve or create conversation
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
        # Title snippet from first message
        title = clean_message[:35] + ("..." if len(clean_message) > 35 else "")
        new_conv = (
            await supabase.table(CHAT_CONVERSATIONS_TABLE)
            .insert({
                "user_id": user_id,
                "role": "student",
                "title": title,
            })
            .execute()
        )
        conversation_id = new_conv.data[0]["id"]

    # 2. Gather student context and recent conversation history
    student_ctx = await build_student_context(user_id=user_id, supabase=supabase)

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
        "You are an encouraging, highly knowledgeable AI Academic Mentor and Personal Tutor for undergraduate "
        "engineering students at Siddhartha Academy. You possess deep knowledge of software engineering, "
        "algorithms, mathematics, and cloud systems. You provide clear, step-by-step conceptual explanations, "
        "code examples where relevant, and motivational study guidance tailored to the student's background."
    )

    full_prompt = f"""
{system_instruction}

---
STUDENT DATA:
{student_ctx}

---
RECENT CHAT HISTORY:
{chr(10).join(formatted_history) if formatted_history else 'No previous conversation in this thread.'}

---
STUDENT QUERY:
{clean_message}

Please reply directly to the student in helpful, clear markdown with concise explanations and actionable advice.
"""

    # 4. Generate AI response via Gemini
    try:
        reply_text = await call_gemini_with_retry(full_prompt, response_mime_type="text/plain")
        if not reply_text:
            reply_text = (
                "I'm here to support your learning journey! Based on your current coursework, "
                "let me know which specific concept or problem you'd like to work through together."
            )
    except Exception as exc:
        logger.error("Gemini student chat generation failed: %s", exc)
        reply_text = (
            "I am currently experiencing higher than usual traffic. In the meantime, review your "
            "course phase resources and I will be ready to help you shortly."
        )

    # 5. Persist both user message and AI response
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

    # Update conversation updated_at
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
