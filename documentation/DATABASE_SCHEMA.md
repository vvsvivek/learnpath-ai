# LearnPath AI — Supabase Database Architecture & Security Guide

## Database Connection
- **Provider**: Supabase PostgreSQL
- **Project Ref**: `munzcpabkjiuxorgmrse`
- **Host**: `aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres`
- **User**: `postgres.munzcpabkjiuxorgmrse`

---

## 1. Relational Entity Schema

### `users`
- Stores student and faculty identity records.
- Fields: `id`, `email`, `password`, `username`, `role` (`STUDENT`, `FACULTY`, `ADMIN`), `branch`, `academic_year`, `semester`, `cgpa`, `is_email_verified`, `auth_provider`, timestamps.

### `email_otps`
- Server-side cryptographic 6-digit OTP codes with timestamps, expiration, and max 5 attempts tracking.
- Fields: `id`, `email`, `otp_code`, `purpose`, `attempts`, `is_used`, `expires_at`, `created_at`.

### `courses`
- Master course enrollment records per student.
- Fields: `id`, `user_id`, `user_email`, `title`, `description`, `difficulty`, `progress`, `current_phase`, `total_phases`, `is_custom`, `is_deleted`, timestamps.

### `course_phases`
- Dynamic subject-specific phases (5 to 8 phases per course).
- Fields: `id`, `course_id`, `phase_number`, `title`, `learning_objectives`, `study_material`, `estimated_duration`, `is_unlocked`, `is_completed`, `completed_at`, timestamps.

### `quiz_questions`
- EXACTLY 10 questions per phase.
- Fields: `id`, `phase_id`, `course_id`, `question_text`, `option_a`, `option_b`, `option_c`, `option_d`, `correct_option` (hidden from student queries before submission), `explanation`, `concept_tag`, timestamps.

### `quiz_attempts`
- Complete assessment attempt records.
- Fields: `id`, `user_id`, `course_id`, `phase_id`, `score`, `total_questions`, `percentage`, `passed` (strictly > 60%), `topic`, `attempt_number`, timestamps.

### `topic_performance`
- Adaptive learning tracking weak topics and failed concepts.
- Fields: `id`, `user_id`, `course_title`, `concept_tag`, `times_tested`, `times_incorrect`, `status` (`NEEDS_ATTENTION`, `DEVELOPING`, `MASTERED`), `last_evaluated_at`.

### `suggested_courses`
- Faculty-assigned departmental courses targeted to specific cohorts.
- Fields: `id`, `faculty_name`, `faculty_email`, `title`, `description`, `target_branch`, `target_year`, `target_semester`, `priority`, `deadline`, timestamps.

### `chat_sessions` & `chat_messages`
- Student and Faculty AI conversation history persisted in PostgreSQL.
- Fields: `id`, `user_email`, `title`, `status`, `context_type`, messages with `sender` and `content`.

---

## 2. Row Level Security (RLS) Execution
Execute the SQL files in `database/` in the Supabase SQL Editor:
1. Run `database/01_schema.sql` to verify tables, constraints, and indexes.
2. Run `database/02_rls_policies.sql` to activate Row Level Security and enforce student/faculty isolation.
3. Run `database/03_seed_data.sql` to insert sample verified test records.
