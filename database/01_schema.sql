-- =================================================================
-- LEARNPATH AI — PRODUCTION SUPABASE POSTGRESQL SCHEMA (01_schema.sql)
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

-- 2. EMAIL OTPS (Server-side OTP state with expiration and attempt tracking)
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

-- 5. QUIZ QUESTIONS TABLE (Server-side questions with hidden correct answers)
CREATE TABLE IF NOT EXISTS quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    phase_id BIGINT REFERENCES course_phases(id) ON DELETE CASCADE,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option VARCHAR(10) NOT NULL, -- 'A', 'B', 'C', 'D'
    explanation TEXT NOT NULL,
    concept_tag VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quiz_questions_phase ON quiz_questions(phase_id);

-- 6. QUIZ ATTEMPTS TABLE (Audited assessment attempts)
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

-- 8. TOPIC PERFORMANCE / ADAPTIVE LEARNING
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

-- 9. FACULTY COURSE RECOMMENDATIONS / ASSIGNMENTS
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
