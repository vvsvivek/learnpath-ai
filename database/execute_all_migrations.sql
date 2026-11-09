-- =================================================================
-- LEARNPATH AI — MASTER SUPABASE SQL SETUP SCRIPT
-- Execute this script in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/munzcpabkjiuxorgmrse/sql
-- Safe, non-destructive, idempotent execution: creates missing tables
-- and configures RLS without deleting or modifying any existing data.
-- =================================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),
    username VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'STUDENT',
    branch VARCHAR(50),
    academic_year VARCHAR(50),
    semester VARCHAR(50),
    cgpa NUMERIC(4,2) DEFAULT 0.00,
    is_email_verified BOOLEAN DEFAULT FALSE,
    auth_provider VARCHAR(50) DEFAULT 'EMAIL',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT check_user_role CHECK (role IN ('STUDENT', 'FACULTY', 'ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_branch_year ON users(branch, academic_year, semester);

-- 2. EMAIL OTPS TABLE
CREATE TABLE IF NOT EXISTS email_otps (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(50) DEFAULT 'VERIFICATION',
    attempts INT DEFAULT 0,
    is_used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_otps_lookup ON email_otps(email, otp_code, is_used);

-- 3. COURSES TABLE
CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    user_email VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    difficulty VARCHAR(50) DEFAULT 'Standard',
    progress INT DEFAULT 0,
    current_phase INT DEFAULT 1,
    total_phases INT DEFAULT 4,
    is_custom BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_courses_user_email ON courses(user_email);
CREATE INDEX IF NOT EXISTS idx_courses_active ON courses(user_email, is_deleted);

-- 4. COURSE PHASES TABLE
CREATE TABLE IF NOT EXISTS course_phases (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE,
    phase_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    learning_objectives TEXT,
    study_material TEXT NOT NULL,
    estimated_duration VARCHAR(50) DEFAULT '2-3 Hours',
    is_unlocked BOOLEAN DEFAULT FALSE,
    is_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_course_phase UNIQUE (course_id, phase_number)
);

CREATE INDEX IF NOT EXISTS idx_course_phases_course ON course_phases(course_id);

-- 5. QUIZ QUESTIONS TABLE
CREATE TABLE IF NOT EXISTS quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    phase_id BIGINT REFERENCES course_phases(id) ON DELETE CASCADE,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option VARCHAR(10) NOT NULL,
    explanation TEXT NOT NULL,
    concept_tag VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quiz_questions_phase ON quiz_questions(phase_id);

-- 6. QUIZ ATTEMPTS TABLE
CREATE TABLE IF NOT EXISTS quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    phase_id BIGINT REFERENCES course_phases(id) ON DELETE CASCADE,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE,
    score INT NOT NULL,
    total_questions INT NOT NULL DEFAULT 10,
    percentage NUMERIC(5,2) NOT NULL,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    topic VARCHAR(255),
    attempt_number INT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user ON quiz_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_phase ON quiz_attempts(phase_id);

-- 7. QUIZ ATTEMPT ANSWERS TABLE
CREATE TABLE IF NOT EXISTS quiz_attempt_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id BIGINT REFERENCES quiz_questions(id) ON DELETE CASCADE,
    selected_option VARCHAR(10) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_attempt_answers_attempt ON quiz_attempt_answers(attempt_id);

-- 8. TOPIC PERFORMANCE TABLE
CREATE TABLE IF NOT EXISTS topic_performance (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    course_title VARCHAR(255) NOT NULL,
    concept_tag VARCHAR(100) NOT NULL,
    times_tested INT DEFAULT 1,
    times_incorrect INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'NEEDS_ATTENTION',
    last_evaluated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_user_topic UNIQUE (user_id, course_title, concept_tag)
);

CREATE INDEX IF NOT EXISTS idx_topic_performance_user ON topic_performance(user_id);

-- 9. SUGGESTED COURSES TABLE
CREATE TABLE IF NOT EXISTS suggested_courses (
    id BIGSERIAL PRIMARY KEY,
    faculty_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    faculty_name VARCHAR(255) NOT NULL,
    faculty_email VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_branch VARCHAR(50) NOT NULL,
    target_year VARCHAR(50) NOT NULL,
    target_semester VARCHAR(50) NOT NULL,
    priority VARCHAR(50) DEFAULT 'HIGH',
    deadline DATE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_suggested_courses_cohort ON suggested_courses(target_branch, target_year, target_semester);

-- 10. CHAT SESSIONS TABLE
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    user_email VARCHAR(255) NOT NULL,
    title VARCHAR(255) DEFAULT 'New Learning Session',
    status VARCHAR(50) DEFAULT 'ACTIVE',
    context_type VARCHAR(50) DEFAULT 'STUDENT',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_sessions_user ON chat_sessions(user_email, status);

-- 11. CHAT MESSAGES TABLE
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_id);

-- =================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =================================================================

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE email_otps ENABLE ROW LEVEL SECURITY;
ALTER TABLE courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE course_phases ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_attempt_answers ENABLE ROW LEVEL SECURITY;
ALTER TABLE topic_performance ENABLE ROW LEVEL SECURITY;
ALTER TABLE suggested_courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_messages ENABLE ROW LEVEL SECURITY;

-- 1. USERS POLICIES
DROP POLICY IF EXISTS student_read_own_profile ON users;
CREATE POLICY student_read_own_profile ON users
    FOR SELECT
    USING (auth.jwt() ->> 'email' = email);

DROP POLICY IF EXISTS faculty_read_eligible_students ON users;
CREATE POLICY faculty_read_eligible_students ON users
    FOR SELECT
    USING (
        (auth.jwt() ->> 'role' = 'FACULTY' OR auth.jwt() ->> 'email' LIKE '%@siddhartha.ac.in' OR auth.jwt() ->> 'email' = 'vivekvaranasi99@gmail.com')
        AND email LIKE '%@vrsec.ac.in'
    );

DROP POLICY IF EXISTS user_update_own_profile ON users;
CREATE POLICY user_update_own_profile ON users
    FOR UPDATE
    USING (auth.jwt() ->> 'email' = email)
    WITH CHECK (auth.jwt() ->> 'email' = email);

-- 2. COURSES POLICIES
DROP POLICY IF EXISTS student_all_own_courses ON courses;
CREATE POLICY student_all_own_courses ON courses
    FOR ALL
    USING (auth.jwt() ->> 'email' = user_email)
    WITH CHECK (auth.jwt() ->> 'email' = user_email);

DROP POLICY IF EXISTS faculty_view_eligible_student_courses ON courses;
CREATE POLICY faculty_view_eligible_student_courses ON courses
    FOR SELECT
    USING (
        (auth.jwt() ->> 'role' = 'FACULTY' OR auth.jwt() ->> 'email' LIKE '%@siddhartha.ac.in' OR auth.jwt() ->> 'email' = 'vivekvaranasi99@gmail.com')
        AND user_email LIKE '%@vrsec.ac.in'
    );

-- 3. COURSE PHASES POLICIES
DROP POLICY IF EXISTS student_view_course_phases ON course_phases;
CREATE POLICY student_view_course_phases ON course_phases
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM courses c
            WHERE c.id = course_phases.course_id
            AND c.user_email = auth.jwt() ->> 'email'
        )
    );

-- 4. QUIZ QUESTIONS POLICIES
DROP POLICY IF EXISTS student_read_phase_questions ON quiz_questions;
CREATE POLICY student_read_phase_questions ON quiz_questions
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM courses c
            WHERE c.id = quiz_questions.course_id
            AND c.user_email = auth.jwt() ->> 'email'
        )
    );

-- 5. QUIZ ATTEMPTS POLICIES
DROP POLICY IF EXISTS student_read_own_attempts ON quiz_attempts;
CREATE POLICY student_read_own_attempts ON quiz_attempts
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = quiz_attempts.user_id
            AND u.email = auth.jwt() ->> 'email'
        )
    );

DROP POLICY IF EXISTS faculty_view_eligible_student_attempts ON quiz_attempts;
CREATE POLICY faculty_view_eligible_student_attempts ON quiz_attempts
    FOR SELECT
    USING (
        (auth.jwt() ->> 'role' = 'FACULTY' OR auth.jwt() ->> 'email' LIKE '%@siddhartha.ac.in' OR auth.jwt() ->> 'email' = 'vivekvaranasi99@gmail.com')
        AND EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = quiz_attempts.user_id
            AND u.email LIKE '%@vrsec.ac.in'
        )
    );

-- 6. TOPIC PERFORMANCE POLICIES
DROP POLICY IF EXISTS student_read_own_topic_performance ON topic_performance;
CREATE POLICY student_read_own_topic_performance ON topic_performance
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = topic_performance.user_id
            AND u.email = auth.jwt() ->> 'email'
        )
    );

DROP POLICY IF EXISTS faculty_read_eligible_topic_performance ON topic_performance;
CREATE POLICY faculty_read_eligible_topic_performance ON topic_performance
    FOR SELECT
    USING (
        (auth.jwt() ->> 'role' = 'FACULTY' OR auth.jwt() ->> 'email' LIKE '%@siddhartha.ac.in' OR auth.jwt() ->> 'email' = 'vivekvaranasi99@gmail.com')
        AND EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = topic_performance.user_id
            AND u.email LIKE '%@vrsec.ac.in'
        )
    );

-- 7. SUGGESTED COURSES POLICIES
DROP POLICY IF EXISTS faculty_manage_suggested_courses ON suggested_courses;
CREATE POLICY faculty_manage_suggested_courses ON suggested_courses
    FOR ALL
    USING (
        auth.jwt() ->> 'email' = faculty_email
        AND (auth.jwt() ->> 'email' LIKE '%@siddhartha.ac.in' OR auth.jwt() ->> 'email' = 'vivekvaranasi99@gmail.com')
    );

DROP POLICY IF EXISTS student_read_matching_recommendations ON suggested_courses;
CREATE POLICY student_read_matching_recommendations ON suggested_courses
    FOR SELECT
    USING (
        auth.jwt() ->> 'email' LIKE '%@vrsec.ac.in'
        AND EXISTS (
            SELECT 1 FROM users u
            WHERE u.email = auth.jwt() ->> 'email'
            AND LOWER(u.branch) = LOWER(suggested_courses.target_branch)
            AND LOWER(u.academic_year) = LOWER(suggested_courses.target_year)
            AND LOWER(u.semester) = LOWER(suggested_courses.target_semester)
        )
    );

-- 8. CHAT SESSIONS & MESSAGES POLICIES
DROP POLICY IF EXISTS user_manage_own_chat_sessions ON chat_sessions;
CREATE POLICY user_manage_own_chat_sessions ON chat_sessions
    FOR ALL
    USING (auth.jwt() ->> 'email' = user_email)
    WITH CHECK (auth.jwt() ->> 'email' = user_email);

DROP POLICY IF EXISTS user_manage_own_chat_messages ON chat_messages;
CREATE POLICY user_manage_own_chat_messages ON chat_messages
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM chat_sessions cs
            WHERE cs.id = chat_messages.session_id
            AND cs.user_email = auth.jwt() ->> 'email'
        )
    );
