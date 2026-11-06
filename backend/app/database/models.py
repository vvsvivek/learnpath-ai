"""
Database Table Names and Schema Documentation.

All database queries are executed via the Supabase client.
This module defines canonical table name constants and schema definitions.
"""

from typing import Final

# ============================================================================
# TABLE NAME CONSTANTS
# ============================================================================

USERS_TABLE: Final[str] = "users"
STUDENT_PROFILES_TABLE: Final[str] = "student_profiles"
FACULTY_PROFILES_TABLE: Final[str] = "faculty_profiles"
OTP_RECORDS_TABLE: Final[str] = "otp_records"

COURSES_TABLE: Final[str] = "courses"
COURSE_ENROLLMENTS_TABLE: Final[str] = "course_enrollments"
LEARNING_PHASES_TABLE: Final[str] = "learning_phases"
PHASE_TOPICS_TABLE: Final[str] = "phase_topics"
RESOURCES_TABLE: Final[str] = "resources"

QUIZZES_TABLE: Final[str] = "quizzes"
QUIZ_QUESTIONS_TABLE: Final[str] = "quiz_questions"
QUIZ_ATTEMPTS_TABLE: Final[str] = "quiz_attempts"

STUDY_SESSIONS_TABLE: Final[str] = "study_sessions"
TOPIC_PERFORMANCE_TABLE: Final[str] = "topic_performance"

CHAT_CONVERSATIONS_TABLE: Final[str] = "chat_conversations"
CHAT_MESSAGES_TABLE: Final[str] = "chat_messages"

FACULTY_COURSE_ASSIGNMENTS_TABLE: Final[str] = "faculty_course_assignments"
COURSE_RECOMMENDATIONS_TABLE: Final[str] = "course_recommendations"
USER_SETTINGS_TABLE: Final[str] = "user_settings"


# ============================================================================
# ROLE & STATUS ENUMS
# ============================================================================

class UserRole:
    STUDENT = "student"
    FACULTY = "faculty"


class EnrollmentStatus:
    ENROLLED = "enrolled"
    IN_PROGRESS = "in_progress"
    COMPLETED = "completed"
    DROPPED = "dropped"


class ResourceType:
    DOCUMENTATION = "documentation"
    VIDEO = "video"
    ARTICLE = "article"
    TUTORIAL = "tutorial"
    GITHUB = "github"
    BOOK = "book"


class PerformanceTier:
    HIGH_PERFORMING = "high_performing"
    AVERAGE = "average"
    NEEDS_SUPPORT = "needs_support"


# ============================================================================
# TABLE STRUCTURE DOCUMENTATION
# ============================================================================
"""
Table Schemas in Supabase / PostgreSQL:

1. users
   - id: UUID PRIMARY KEY (gen_random_uuid())
   - email: VARCHAR(255) UNIQUE NOT NULL
   - role: VARCHAR(50) NOT NULL ('student' | 'faculty')
   - is_active: BOOLEAN DEFAULT TRUE
   - created_at: TIMESTAMPTZ DEFAULT NOW()
   - updated_at: TIMESTAMPTZ DEFAULT NOW()

2. student_profiles
   - id: UUID PRIMARY KEY (gen_random_uuid())
   - user_id: UUID UNIQUE REFERENCES users(id) ON DELETE CASCADE
   - full_name: VARCHAR(255) NOT NULL
   - branch: VARCHAR(100) NOT NULL
   - academic_year: INTEGER NOT NULL
   - semester: INTEGER NOT NULL
   - cgpa: NUMERIC(4, 2)
   - learning_interests: JSONB / TEXT[] DEFAULT '[]'
   - created_at: TIMESTAMPTZ DEFAULT NOW()
   - updated_at: TIMESTAMPTZ DEFAULT NOW()

3. faculty_profiles
   - id: UUID PRIMARY KEY (gen_random_uuid())
   - user_id: UUID UNIQUE REFERENCES users(id) ON DELETE CASCADE
   - full_name: VARCHAR(255) NOT NULL
   - department: VARCHAR(100) NOT NULL
   - designation: VARCHAR(100)
   - created_at: TIMESTAMPTZ DEFAULT NOW()
   - updated_at: TIMESTAMPTZ DEFAULT NOW()

4. otp_records
   - id: UUID PRIMARY KEY (gen_random_uuid())
   - email: VARCHAR(255) NOT NULL
   - otp_hash: VARCHAR(255) NOT NULL
   - role: VARCHAR(50) NOT NULL
   - purpose: VARCHAR(100) DEFAULT 'login'
   - attempts: INTEGER DEFAULT 0
   - is_used: BOOLEAN DEFAULT FALSE
   - expires_at: TIMESTAMPTZ NOT NULL
   - created_at: TIMESTAMPTZ DEFAULT NOW()

5. courses
   - id: UUID PRIMARY KEY (gen_random_uuid())
   - title: VARCHAR(255) NOT NULL
   - description: TEXT
   - category: VARCHAR(100)
   - difficulty: VARCHAR(50) DEFAULT 'Beginner'
   - is_custom: BOOLEAN DEFAULT FALSE
   - created_by: UUID REFERENCES users(id) ON DELETE SET NULL
   - created_at: TIMESTAMPTZ DEFAULT NOW()
   - updated_at: TIMESTAMPTZ DEFAULT NOW()

6. course_enrollments
   - id: UUID PRIMARY KEY (gen_random_uuid())
   - user_id: UUID REFERENCES users(id) ON DELETE CASCADE
   - course_id: UUID REFERENCES courses(id) ON DELETE CASCADE
   - status: VARCHAR(50) DEFAULT 'enrolled'
   - progress_percentage: NUMERIC(5, 2) DEFAULT 0.00
   - enrolled_at: TIMESTAMPTZ DEFAULT NOW()
   - completed_at: TIMESTAMPTZ

7. learning_phases
   - id: UUID PRIMARY KEY (gen_random_uuid())
   - course_id: UUID REFERENCES courses(id) ON DELETE CASCADE
   - phase_number: INTEGER NOT NULL
   - title: VARCHAR(255) NOT NULL
   - description: TEXT
   - estimated_hours: NUMERIC(5, 1) DEFAULT 10.0
   - created_at: TIMESTAMPTZ DEFAULT NOW()

8. phase_topics
   - id: UUID PRIMARY KEY (gen_random_uuid())
   - phase_id: UUID REFERENCES learning_phases(id) ON DELETE CASCADE
   - topic_number: INTEGER NOT NULL
   - title: VARCHAR(255) NOT NULL
   - description: TEXT
   - created_at: TIMESTAMPTZ DEFAULT NOW()

9. resources
   - id: UUID PRIMARY KEY (gen_random_uuid())
   - topic_id: UUID REFERENCES phase_topics(id) ON DELETE CASCADE
   - title: VARCHAR(255) NOT NULL
   - url: TEXT NOT NULL
   - resource_type: VARCHAR(50) DEFAULT 'article'
   - estimated_minutes: INTEGER DEFAULT 20
   - created_at: TIMESTAMPTZ DEFAULT NOW()

10. quizzes
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - phase_id: UUID REFERENCES learning_phases(id) ON DELETE CASCADE
    - title: VARCHAR(255) NOT NULL
    - description: TEXT
    - time_limit_minutes: INTEGER DEFAULT 15
    - created_at: TIMESTAMPTZ DEFAULT NOW()

11. quiz_questions
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - quiz_id: UUID REFERENCES quizzes(id) ON DELETE CASCADE
    - question_text: TEXT NOT NULL
    - options: JSONB NOT NULL
    - correct_option: INTEGER NOT NULL
    - explanation: TEXT
    - created_at: TIMESTAMPTZ DEFAULT NOW()

12. quiz_attempts
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - user_id: UUID REFERENCES users(id) ON DELETE CASCADE
    - quiz_id: UUID REFERENCES quizzes(id) ON DELETE CASCADE
    - score: INTEGER NOT NULL
    - total_questions: INTEGER NOT NULL
    - percentage: NUMERIC(5, 2) NOT NULL
    - passed: BOOLEAN DEFAULT FALSE
    - answers: JSONB
    - completed_at: TIMESTAMPTZ DEFAULT NOW()

13. study_sessions
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - user_id: UUID REFERENCES users(id) ON DELETE CASCADE
    - course_id: UUID REFERENCES courses(id) ON DELETE SET NULL
    - start_time: TIMESTAMPTZ NOT NULL DEFAULT NOW()
    - end_time: TIMESTAMPTZ
    - duration_minutes: INTEGER DEFAULT 0
    - notes: TEXT
    - created_at: TIMESTAMPTZ DEFAULT NOW()

14. topic_performance
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - user_id: UUID REFERENCES users(id) ON DELETE CASCADE
    - topic_id: UUID REFERENCES phase_topics(id) ON DELETE CASCADE
    - is_completed: BOOLEAN DEFAULT FALSE
    - mastery_level: VARCHAR(50) DEFAULT 'needs_practice'
    - last_quiz_score: NUMERIC(5, 2)
    - updated_at: TIMESTAMPTZ DEFAULT NOW()

15. chat_conversations
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - user_id: UUID REFERENCES users(id) ON DELETE CASCADE
    - role: VARCHAR(50) NOT NULL
    - title: VARCHAR(255) DEFAULT 'New Conversation'
    - created_at: TIMESTAMPTZ DEFAULT NOW()
    - updated_at: TIMESTAMPTZ DEFAULT NOW()

16. chat_messages
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - conversation_id: UUID REFERENCES chat_conversations(id) ON DELETE CASCADE
    - sender: VARCHAR(50) NOT NULL
    - message: TEXT NOT NULL
    - created_at: TIMESTAMPTZ DEFAULT NOW()

17. faculty_course_assignments
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - faculty_id: UUID REFERENCES users(id) ON DELETE CASCADE
    - course_id: UUID REFERENCES courses(id) ON DELETE CASCADE
    - academic_year: INTEGER NOT NULL
    - branch: VARCHAR(100) NOT NULL
    - semester: INTEGER NOT NULL
    - due_date: TIMESTAMPTZ
    - assigned_at: TIMESTAMPTZ DEFAULT NOW()

18. course_recommendations
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - user_id: UUID REFERENCES users(id) ON DELETE CASCADE
    - course_title: VARCHAR(255) NOT NULL
    - description: TEXT
    - category: VARCHAR(100)
    - difficulty: VARCHAR(50)
    - reason: TEXT
    - match_score: NUMERIC(4, 2) DEFAULT 0.90
    - is_dismissed: BOOLEAN DEFAULT FALSE
    - created_at: TIMESTAMPTZ DEFAULT NOW()

19. user_settings
    - id: UUID PRIMARY KEY (gen_random_uuid())
    - user_id: UUID UNIQUE REFERENCES users(id) ON DELETE CASCADE
    - email_notifications: BOOLEAN DEFAULT TRUE
    - theme: VARCHAR(50) DEFAULT 'light'
    - updated_at: TIMESTAMPTZ DEFAULT NOW()
"""
