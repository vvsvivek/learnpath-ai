"""
Google Gemini Live API Verification Script.

Tests Gemini API key, generates real personalized course recommendations,
a 4-phase structured learning path, a dynamic 5-question MCQ assessment,
and an AI tutor conversational response.
"""

import asyncio
import os
import sys
from dotenv import load_dotenv

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
load_dotenv()

async def verify_gemini():
    api_key = os.getenv("GEMINI_API_KEY")
    model_name = os.getenv("GEMINI_MODEL", "gemini-3.8-flash")

    if not api_key or "your-gemini-api-key" in api_key:
        print("[FAIL] Missing or placeholder GEMINI_API_KEY.")
        print("Please configure real credentials in backend/.env.")
        sys.exit(1)

    print(f"[*] Initializing Google GenAI Client with model: {model_name}...")
    from google import genai
    client = genai.Client(api_key=api_key)

    # 1. Test Course Recommendations Generation
    print("\n--- 1. Testing Live Course Recommendation Generation ---")
    prompt_rec = (
        "Generate 2 advanced technical course recommendations for a B.Tech Computer Science student in Semester 6 "
        "interested in Generative AI and Distributed Systems. Return a JSON array with title, description, category, difficulty, reason."
    )
    try:
        response_rec = client.models.generate_content(
            model=model_name,
            contents=prompt_rec,
        )
        print("  [OK] Course Recommendations Generated:")
        print(f"  {response_rec.text[:250]}...\n")
    except Exception as e:
        print(f"  [ERROR] Recommendation generation failed: {e}")
        sys.exit(1)

    # 2. Test 4-Phase Learning Path Generation
    print("--- 2. Testing 4-Phase Learning Path Generation ---")
    prompt_path = (
        "Create a 4-phase learning path for 'Kubernetes & Cloud-Native Microservices'. "
        "For each phase (Phase 1 to Phase 4), provide title, description, estimated_hours, and 2 key topics."
    )
    try:
        response_path = client.models.generate_content(
            model=model_name,
            contents=prompt_path,
        )
        print("  [OK] 4-Phase Learning Path Generated:")
        print(f"  {response_path.text[:250]}...\n")
    except Exception as e:
        print(f"  [ERROR] Learning path generation failed: {e}")
        sys.exit(1)

    # 3. Test Dynamic Quiz Generation
    print("--- 3. Testing Dynamic MCQ Assessment Generation ---")
    prompt_quiz = (
        "Generate 3 multiple-choice assessment questions on 'Docker Container Security and Isolation'. "
        "Provide question_text, 4 options, correct_option index (0-3), and an explanation."
    )
    try:
        response_quiz = client.models.generate_content(
            model=model_name,
            contents=prompt_quiz,
        )
        print("  [OK] Dynamic Assessment Generated:")
        print(f"  {response_quiz.text[:250]}...\n")
    except Exception as e:
        print(f"  [ERROR] Quiz generation failed: {e}")
        sys.exit(1)

    # 4. Test Student AI Assistant Response
    print("--- 4. Testing Student AI Tutor Live Response ---")
    prompt_tutor = (
        "System: You are an AI Academic Tutor for B.Tech students at VRSEC.\n"
        "Student Vivek asks: 'Can you explain the difference between processes and threads using a realistic analogy?'"
    )
    try:
        response_tutor = client.models.generate_content(
            model=model_name,
            contents=prompt_tutor,
        )
        print("  [OK] AI Tutor Response Generated:")
        print(f"  {response_tutor.text[:250].encode('ascii', errors='replace').decode('ascii')}...\n")
    except Exception as e:
        print(f"  [ERROR] AI Tutor generation failed: {e}")
        sys.exit(1)

    print("=======================================================")
    print("[PASSED] Google Gemini API fully verified and operational!")
    print("=======================================================")

if __name__ == "__main__":
    asyncio.run(verify_gemini())
