-- =================================================================
-- LEARNPATH AI — MASTER SUPABASE SQL SETUP SCRIPT
-- Execute this script in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/munzcpabkjiuxorgmrse/sql
-- Safe, non-destructive, idempotent execution: creates missing tables
-- and configures RLS without deleting or modifying any existing data.
-- =================================================================

-- =================================================================
-- SECTION 0: RELATIONAL COMPATIBILITY MIGRATION (UUID TO BIGINT)
-- Safely migrates legacy UUID primary and foreign keys to BIGINT sequences
-- while strictly preserving all existing row-to-row relationships.
-- =================================================================
DO $$
DECLARE
    users_is_uuid boolean := false;
    courses_is_uuid boolean := false;
    phases_is_uuid boolean := false;
    questions_is_uuid boolean := false;
    attempts_is_uuid boolean := false;
    performance_is_uuid boolean := false;
    sessions_is_uuid boolean := false;
    messages_is_uuid boolean := false;
    paths_is_uuid boolean := false;

    pk_name text;
    r RECORD;
BEGIN
    -- 0. Inspect existing column types
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        SELECT (data_type = 'uuid') INTO users_is_uuid FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courses') THEN
        SELECT (data_type = 'uuid') INTO courses_is_uuid FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'course_phases') THEN
        SELECT (data_type = 'uuid') INTO phases_is_uuid FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quiz_questions') THEN
        SELECT (data_type = 'uuid') INTO questions_is_uuid FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quiz_attempts') THEN
        SELECT (data_type = 'uuid') INTO attempts_is_uuid FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'topic_performance') THEN
        SELECT (data_type = 'uuid') INTO performance_is_uuid FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_sessions') THEN
        SELECT (data_type = 'uuid') INTO sessions_is_uuid FROM information_schema.columns WHERE table_name = 'chat_sessions' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_messages') THEN
        SELECT (data_type = 'uuid') INTO messages_is_uuid FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'learning_paths') THEN
        SELECT (data_type = 'uuid') INTO paths_is_uuid FROM information_schema.columns WHERE table_name = 'learning_paths' AND column_name = 'id';
    END IF;

    -- 1. Drop all legacy foreign key constraints
    FOR r IN (
        SELECT tc.table_schema, tc.table_name, tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name
        WHERE tc.constraint_type = 'FOREIGN KEY'
          AND ccu.table_name IN ('courses', 'users', 'course_phases', 'chat_sessions', 'quiz_questions', 'quiz_attempts', 'topic_performance', 'learning_paths')
    ) LOOP
        EXECUTE 'ALTER TABLE ' || quote_ident(r.table_schema) || '.' || quote_ident(r.table_name) || ' DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
    END LOOP;

    -- 2. Prepare BIGINT IDs
    IF users_is_uuid THEN ALTER TABLE users ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; END IF;
    IF courses_is_uuid THEN ALTER TABLE courses ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; END IF;
    IF phases_is_uuid THEN ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; END IF;
    IF sessions_is_uuid THEN ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; END IF;
    IF questions_is_uuid THEN ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; END IF;
    IF attempts_is_uuid THEN ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; END IF;
    IF performance_is_uuid THEN ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; END IF;
    IF messages_is_uuid THEN ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; END IF;
    IF paths_is_uuid THEN ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; END IF;

    -- 3. Relational Mapping for user_id
    IF users_is_uuid THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'user_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_user_id BIGINT;
            UPDATE quiz_attempts qa SET new_user_id = u.new_id FROM users u WHERE qa.user_id = u.id;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_sessions' AND column_name = 'user_id' AND data_type = 'uuid') THEN
            ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS new_user_id BIGINT;
            UPDATE chat_sessions cs SET new_user_id = u.new_id FROM users u WHERE cs.user_id = u.id;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'user_id' AND data_type = 'uuid') THEN
            ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS new_user_id BIGINT;
            UPDATE topic_performance tp SET new_user_id = u.new_id FROM users u WHERE tp.user_id = u.id;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'learning_paths' AND column_name = 'student_id' AND data_type = 'uuid') THEN
            ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS new_student_id BIGINT;
            UPDATE learning_paths lp SET new_student_id = u.new_id FROM users u WHERE lp.student_id = u.id;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'user_id' AND data_type = 'uuid') THEN
            ALTER TABLE courses ADD COLUMN IF NOT EXISTS new_user_id BIGINT;
            UPDATE courses c SET new_user_id = u.new_id FROM users u WHERE c.user_id = u.id;
        END IF;
    END IF;

    -- 4. Relational Mapping for course_id
    IF courses_is_uuid THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'course_id' AND data_type = 'uuid') THEN
            ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS new_course_id BIGINT;
            UPDATE course_phases cp SET new_course_id = c.new_id FROM courses c WHERE cp.course_id = c.id;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'course_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS new_course_id BIGINT;
            UPDATE quiz_questions qq SET new_course_id = c.new_id FROM courses c WHERE qq.course_id = c.id;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'course_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_course_id BIGINT;
            UPDATE quiz_attempts qa SET new_course_id = c.new_id FROM courses c WHERE qa.course_id = c.id;
        END IF;
    END IF;

    -- 5. Relational Mapping for phase_id
    IF phases_is_uuid THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'phase_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS new_phase_id BIGINT;
            UPDATE quiz_questions qq SET new_phase_id = cp.new_id FROM course_phases cp WHERE qq.phase_id = cp.id;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'phase_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_phase_id BIGINT;
            UPDATE quiz_attempts qa SET new_phase_id = cp.new_id FROM course_phases cp WHERE qa.phase_id = cp.id;
        END IF;
    END IF;

    -- 6. Relational Mapping for session_id
    IF sessions_is_uuid THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'session_id' AND data_type = 'uuid') THEN
            ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS new_session_id BIGINT;
            UPDATE chat_messages cm SET new_session_id = cs.new_id FROM chat_sessions cs WHERE cm.session_id = cs.id;
        END IF;
    END IF;

    -- 7. Swap Primary Keys
    IF users_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'users' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN EXECUTE 'ALTER TABLE users DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE'; END IF;
        ALTER TABLE users DROP COLUMN id;
        ALTER TABLE users RENAME COLUMN new_id TO id;
        ALTER TABLE users ADD PRIMARY KEY (id);
    END IF;

    IF courses_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'courses' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN EXECUTE 'ALTER TABLE courses DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE'; END IF;
        ALTER TABLE courses DROP COLUMN id;
        ALTER TABLE courses RENAME COLUMN new_id TO id;
        ALTER TABLE courses ADD PRIMARY KEY (id);
    END IF;

    IF phases_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'course_phases' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN EXECUTE 'ALTER TABLE course_phases DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE'; END IF;
        ALTER TABLE course_phases DROP COLUMN id;
        ALTER TABLE course_phases RENAME COLUMN new_id TO id;
        ALTER TABLE course_phases ADD PRIMARY KEY (id);
    END IF;

    IF questions_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'quiz_questions' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN EXECUTE 'ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE'; END IF;
        ALTER TABLE quiz_questions DROP COLUMN id;
        ALTER TABLE quiz_questions RENAME COLUMN new_id TO id;
        ALTER TABLE quiz_questions ADD PRIMARY KEY (id);
    END IF;

    IF attempts_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'quiz_attempts' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN EXECUTE 'ALTER TABLE quiz_attempts DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE'; END IF;
        ALTER TABLE quiz_attempts DROP COLUMN id;
        ALTER TABLE quiz_attempts RENAME COLUMN new_id TO id;
        ALTER TABLE quiz_attempts ADD PRIMARY KEY (id);
    END IF;

    IF performance_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'topic_performance' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN EXECUTE 'ALTER TABLE topic_performance DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE'; END IF;
        ALTER TABLE topic_performance DROP COLUMN id;
        ALTER TABLE topic_performance RENAME COLUMN new_id TO id;
        ALTER TABLE topic_performance ADD PRIMARY KEY (id);
    END IF;

    IF sessions_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'chat_sessions' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN EXECUTE 'ALTER TABLE chat_sessions DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE'; END IF;
        ALTER TABLE chat_sessions DROP COLUMN id;
        ALTER TABLE chat_sessions RENAME COLUMN new_id TO id;
        ALTER TABLE chat_sessions ADD PRIMARY KEY (id);
    END IF;

    IF messages_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'chat_messages' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN EXECUTE 'ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE'; END IF;
        ALTER TABLE chat_messages DROP COLUMN id;
        ALTER TABLE chat_messages RENAME COLUMN new_id TO id;
        ALTER TABLE chat_messages ADD PRIMARY KEY (id);
    END IF;

    IF paths_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'learning_paths' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN EXECUTE 'ALTER TABLE learning_paths DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE'; END IF;
        ALTER TABLE learning_paths DROP COLUMN id;
        ALTER TABLE learning_paths RENAME COLUMN new_id TO id;
        ALTER TABLE learning_paths ADD PRIMARY KEY (id);
    END IF;

    -- 8. Swap Foreign Key Columns
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'new_user_id') THEN
        ALTER TABLE quiz_attempts DROP COLUMN user_id; ALTER TABLE quiz_attempts RENAME COLUMN new_user_id TO user_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'new_course_id') THEN
        ALTER TABLE quiz_attempts DROP COLUMN course_id; ALTER TABLE quiz_attempts RENAME COLUMN new_course_id TO course_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'new_phase_id') THEN
        ALTER TABLE quiz_attempts DROP COLUMN phase_id; ALTER TABLE quiz_attempts RENAME COLUMN new_phase_id TO phase_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'new_course_id') THEN
        ALTER TABLE course_phases DROP COLUMN course_id; ALTER TABLE course_phases RENAME COLUMN new_course_id TO course_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'new_course_id') THEN
        ALTER TABLE quiz_questions DROP COLUMN course_id; ALTER TABLE quiz_questions RENAME COLUMN new_course_id TO course_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'new_phase_id') THEN
        ALTER TABLE quiz_questions DROP COLUMN phase_id; ALTER TABLE quiz_questions RENAME COLUMN new_phase_id TO phase_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_sessions' AND column_name = 'new_user_id') THEN
        ALTER TABLE chat_sessions DROP COLUMN user_id; ALTER TABLE chat_sessions RENAME COLUMN new_user_id TO user_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'new_session_id') THEN
        ALTER TABLE chat_messages DROP COLUMN session_id; ALTER TABLE chat_messages RENAME COLUMN new_session_id TO session_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'new_user_id') THEN
        ALTER TABLE topic_performance DROP COLUMN user_id; ALTER TABLE topic_performance RENAME COLUMN new_user_id TO user_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'learning_paths' AND column_name = 'new_student_id') THEN
        ALTER TABLE learning_paths DROP COLUMN student_id; ALTER TABLE learning_paths RENAME COLUMN new_student_id TO student_id;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'new_user_id') THEN
        ALTER TABLE courses DROP COLUMN user_id; ALTER TABLE courses RENAME COLUMN new_user_id TO user_id;
    END IF;
END $$;

-- =================================================================
-- SECTION 1: CORE TABLES DEFINITION (IDEMPOTENT CREATION)
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
