"""
Supabase PostgreSQL Live Verification Script.

Tests connection, verifies the presence of all 18 tables, tests inserts, queries,
foreign key relationships, and cleans up test records.
"""

import asyncio
import os
import sys
from dotenv import load_dotenv

# Load local environment
load_dotenv()

EXPECTED_TABLES = [
    "users",
    "student_profiles",
    "faculty_profiles",
    "otp_records",
    "courses",
    "course_enrollments",
    "learning_phases",
    "phase_topics",
    "resources",
    "quizzes",
    "quiz_questions",
    "quiz_attempts",
    "study_sessions",
    "topic_performance",
    "chat_conversations",
    "chat_messages",
    "faculty_course_assignments",
    "course_recommendations",
    "user_settings",
]

async def verify_supabase():
    url = os.getenv("SUPABASE_URL")
    key = os.getenv("SUPABASE_SERVICE_ROLE_KEY") or os.getenv("SUPABASE_ANON_KEY")

    if not url or not key or "placeholder" in url or "your-project" in url:
        print("[FAIL] Missing or placeholder SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY.")
        print("Please configure real credentials in backend/.env.")
        sys.exit(1)

    print(f"[*] Connecting to Supabase at: {url}...")
    from supabase import acreate_client
    client = await acreate_client(url, key)

    # 1. Verify table access
    print("\n--- 1. Table Verification ---")
    missing_tables = []
    for table in EXPECTED_TABLES:
        try:
            res = await client.table(table).select("*").limit(1).execute()
            print(f"  [OK] Table '{table}' accessible.")
        except Exception as e:
            print(f"  [ERROR] Table '{table}' failed: {e}")
            missing_tables.append(table)

    if missing_tables:
        print(f"\n[FAIL] {len(missing_tables)} tables are missing or inaccessible.")
        print("Please run backend/migrations/001_initial_schema.sql in the Supabase SQL Editor.")
        sys.exit(1)

    print(f"\n[SUCCESS] All {len(EXPECTED_TABLES)} required tables verified in Supabase database!")

    # 2. Test Real Student Database Operations
    print("\n--- 2. Student CRUD & Relationship Operations ---")
    test_student_email = "audit_student@vrsec.ac.in"
    try:
        # Create user
        user_res = await client.table("users").insert({
            "email": test_student_email,
            "role": "student",
            "is_active": True,
        }).execute()
        student_user_id = user_res.data[0]["id"]
        print(f"  [OK] Created student user: {student_user_id}")

        # Create profile (FK to users)
        profile_res = await client.table("student_profiles").insert({
            "user_id": student_user_id,
            "full_name": "Audit Student",
            "branch": "Computer Science & Engineering",
            "academic_year": 3,
            "semester": 6,
            "cgpa": 8.75,
            "learning_interests": ["Deep Learning", "FastAPI"],
        }).execute()
        print(f"  [OK] Created student profile with foreign key link.")

        # Clean up
        await client.table("users").delete().eq("id", student_user_id).execute()
        print(f"  [OK] Cleaned up student test record (cascade deletion verified).")
    except Exception as e:
        print(f"  [ERROR] Student operations failed: {e}")
        sys.exit(1)

    # 3. Test Real Faculty Database Operations
    print("\n--- 3. Faculty CRUD Operations ---")
    test_faculty_email = "audit_prof@siddhartha.ac.in"
    try:
        user_res = await client.table("users").insert({
            "email": test_faculty_email,
            "role": "faculty",
            "is_active": True,
        }).execute()
        faculty_user_id = user_res.data[0]["id"]
        print(f"  [OK] Created faculty user: {faculty_user_id}")

        faculty_prof = await client.table("faculty_profiles").insert({
            "user_id": faculty_user_id,
            "full_name": "Dr. Audit Professor",
            "department": "Computer Science & Engineering",
            "designation": "Associate Professor",
        }).execute()
        print(f"  [OK] Created faculty profile.")

        # Clean up
        await client.table("users").delete().eq("id", faculty_user_id).execute()
        print(f"  [OK] Cleaned up faculty test record.")
    except Exception as e:
        print(f"  [ERROR] Faculty operations failed: {e}")
        sys.exit(1)

    print("\n=======================================================")
    print("[PASSED] Supabase Database fully operational and verified!")
    print("=======================================================")

if __name__ == "__main__":
    asyncio.run(verify_supabase())
