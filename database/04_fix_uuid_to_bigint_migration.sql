-- =================================================================
-- LEARNPATH AI — PRODUCTION RELATIONAL MIGRATION: UUID TO BIGINT
-- File: database/04_fix_uuid_to_bigint_migration.sql
-- Purpose: Safely migrates all legacy UUID primary and foreign keys to
--          BIGSERIAL / BIGINT while strictly preserving all row-to-row
--          relational integrity, user records, and progress data.
-- Properties:
--   1. Relational Integrity: All child foreign keys are mapped directly
--      to their corresponding parent's new BIGINT ID before UUID columns are retired.
--   2. Atomic & Reversible: Entire block runs in a single PL/pgSQL transaction;
--      any failure causes a complete rollback with zero partial state.
--   3. Idempotent: Checks information_schema column data types. If columns
--      are already BIGINT/integer, migration steps are safely skipped.
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
    -- -------------------------------------------------------------
    -- 0. INSPECT EXISTING COLUMN TYPES
    -- -------------------------------------------------------------
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        SELECT (data_type = 'uuid') INTO users_is_uuid 
        FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courses') THEN
        SELECT (data_type = 'uuid') INTO courses_is_uuid 
        FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'course_phases') THEN
        SELECT (data_type = 'uuid') INTO phases_is_uuid 
        FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quiz_questions') THEN
        SELECT (data_type = 'uuid') INTO questions_is_uuid 
        FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quiz_attempts') THEN
        SELECT (data_type = 'uuid') INTO attempts_is_uuid 
        FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'topic_performance') THEN
        SELECT (data_type = 'uuid') INTO performance_is_uuid 
        FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_sessions') THEN
        SELECT (data_type = 'uuid') INTO sessions_is_uuid 
        FROM information_schema.columns WHERE table_name = 'chat_sessions' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_messages') THEN
        SELECT (data_type = 'uuid') INTO messages_is_uuid 
        FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'id';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'learning_paths') THEN
        SELECT (data_type = 'uuid') INTO paths_is_uuid 
        FROM information_schema.columns WHERE table_name = 'learning_paths' AND column_name = 'id';
    END IF;

    -- -------------------------------------------------------------
    -- 1. DROP ALL EXISTING FOREIGN KEY CONSTRAINTS TO UNBLOCK ALTERATIONS
    -- -------------------------------------------------------------
    FOR r IN (
        SELECT tc.table_schema, tc.table_name, tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name
        WHERE tc.constraint_type = 'FOREIGN KEY'
          AND ccu.table_name IN ('courses', 'users', 'course_phases', 'chat_sessions', 'quiz_questions', 'quiz_attempts', 'topic_performance', 'learning_paths')
    ) LOOP
        EXECUTE 'ALTER TABLE ' || quote_ident(r.table_schema) || '.' || quote_ident(r.table_name) || ' DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
    END LOOP;

    -- 1b. DROP ALL LEGACY RESTRICTIVE CHECK CONSTRAINTS ACROSS TABLES
    FOR r IN (
        SELECT table_schema, table_name, constraint_name
        FROM information_schema.table_constraints
        WHERE constraint_type = 'CHECK'
          AND table_name IN ('users', 'chat_messages', 'courses', 'course_phases', 'chat_sessions', 'quiz_attempts', 'topic_performance')
          AND constraint_name NOT LIKE '%not_null%'
    ) LOOP
        EXECUTE 'ALTER TABLE ' || quote_ident(r.table_schema) || '.' || quote_ident(r.table_name) || ' DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
    END LOOP;

    -- -------------------------------------------------------------
    -- 2. PHASE 1: PREPARE BIGINT IDS ON PRIMARY ENTITIES
    -- -------------------------------------------------------------
    IF users_is_uuid THEN
        ALTER TABLE users ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
    END IF;

    IF courses_is_uuid THEN
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
    END IF;

    IF phases_is_uuid THEN
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
    END IF;

    IF sessions_is_uuid THEN
        ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
    END IF;

    IF questions_is_uuid THEN
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
    END IF;

    IF attempts_is_uuid THEN
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
    END IF;

    IF performance_is_uuid THEN
        ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
    END IF;

    IF messages_is_uuid THEN
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
    END IF;

    IF paths_is_uuid THEN
        ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
    END IF;

    -- -------------------------------------------------------------
    -- 3. PHASE 2: RELATIONAL MAPPING FOR USER_ID REFERENCES
    -- -------------------------------------------------------------
    IF users_is_uuid THEN
        -- quiz_attempts -> users
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'user_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_user_id BIGINT;
            UPDATE quiz_attempts qa SET new_user_id = u.new_id FROM users u WHERE qa.user_id = u.id;
        END IF;

        -- chat_sessions -> users
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_sessions' AND column_name = 'user_id' AND data_type = 'uuid') THEN
            ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS new_user_id BIGINT;
            UPDATE chat_sessions cs SET new_user_id = u.new_id FROM users u WHERE cs.user_id = u.id;
        END IF;

        -- topic_performance -> users
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'user_id' AND data_type = 'uuid') THEN
            ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS new_user_id BIGINT;
            UPDATE topic_performance tp SET new_user_id = u.new_id FROM users u WHERE tp.user_id = u.id;
        END IF;

        -- learning_paths -> users
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'learning_paths' AND column_name = 'student_id' AND data_type = 'uuid') THEN
            ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS new_student_id BIGINT;
            UPDATE learning_paths lp SET new_student_id = u.new_id FROM users u WHERE lp.student_id = u.id;
        END IF;

        -- courses -> users (if user_id exists as uuid)
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'user_id' AND data_type = 'uuid') THEN
            ALTER TABLE courses ADD COLUMN IF NOT EXISTS new_user_id BIGINT;
            UPDATE courses c SET new_user_id = u.new_id FROM users u WHERE c.user_id = u.id;
        END IF;
    END IF;

    -- -------------------------------------------------------------
    -- 4. PHASE 3: RELATIONAL MAPPING FOR COURSE_ID REFERENCES
    -- -------------------------------------------------------------
    IF courses_is_uuid THEN
        -- course_phases -> courses
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'course_id' AND data_type = 'uuid') THEN
            ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS new_course_id BIGINT;
            UPDATE course_phases cp SET new_course_id = c.new_id FROM courses c WHERE cp.course_id = c.id;
        END IF;

        -- quiz_questions -> courses
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'course_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS new_course_id BIGINT;
            UPDATE quiz_questions qq SET new_course_id = c.new_id FROM courses c WHERE qq.course_id = c.id;
        END IF;

        -- quiz_attempts -> courses
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'course_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_course_id BIGINT;
            UPDATE quiz_attempts qa SET new_course_id = c.new_id FROM courses c WHERE qa.course_id = c.id;
        END IF;
    END IF;

    -- -------------------------------------------------------------
    -- 5. PHASE 4: RELATIONAL MAPPING FOR PHASE_ID REFERENCES
    -- -------------------------------------------------------------
    IF phases_is_uuid THEN
        -- quiz_questions -> course_phases
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'phase_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS new_phase_id BIGINT;
            UPDATE quiz_questions qq SET new_phase_id = cp.new_id FROM course_phases cp WHERE qq.phase_id = cp.id;
        END IF;

        -- quiz_attempts -> course_phases
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'phase_id' AND data_type = 'uuid') THEN
            ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_phase_id BIGINT;
            UPDATE quiz_attempts qa SET new_phase_id = cp.new_id FROM course_phases cp WHERE qa.phase_id = cp.id;
        END IF;
    END IF;

    -- -------------------------------------------------------------
    -- 6. PHASE 5: RELATIONAL MAPPING FOR SESSION_ID REFERENCES
    -- -------------------------------------------------------------
    IF sessions_is_uuid THEN
        -- chat_messages -> chat_sessions
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'session_id' AND data_type = 'uuid') THEN
            ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS new_session_id BIGINT;
            UPDATE chat_messages cm SET new_session_id = cs.new_id FROM chat_sessions cs WHERE cm.session_id = cs.id;
        END IF;
    END IF;

    -- -------------------------------------------------------------
    -- 7. PHASE 6: SWAP PRIMARY KEYS (UUID -> BIGSERIAL ID)
    -- -------------------------------------------------------------
    -- USERS
    IF users_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'users' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE users DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
        END IF;
        ALTER TABLE users DROP COLUMN id;
        ALTER TABLE users RENAME COLUMN new_id TO id;
        ALTER TABLE users ADD PRIMARY KEY (id);
    END IF;

    -- COURSES
    IF courses_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'courses' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE courses DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
        END IF;
        ALTER TABLE courses DROP COLUMN id;
        ALTER TABLE courses RENAME COLUMN new_id TO id;
        ALTER TABLE courses ADD PRIMARY KEY (id);
    END IF;

    -- COURSE_PHASES
    IF phases_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'course_phases' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE course_phases DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
        END IF;
        ALTER TABLE course_phases DROP COLUMN id;
        ALTER TABLE course_phases RENAME COLUMN new_id TO id;
        ALTER TABLE course_phases ADD PRIMARY KEY (id);
    END IF;

    -- QUIZ_QUESTIONS
    IF questions_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'quiz_questions' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
        END IF;
        ALTER TABLE quiz_questions DROP COLUMN id;
        ALTER TABLE quiz_questions RENAME COLUMN new_id TO id;
        ALTER TABLE quiz_questions ADD PRIMARY KEY (id);
    END IF;

    -- QUIZ_ATTEMPTS
    IF attempts_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'quiz_attempts' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE quiz_attempts DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
        END IF;
        ALTER TABLE quiz_attempts DROP COLUMN id;
        ALTER TABLE quiz_attempts RENAME COLUMN new_id TO id;
        ALTER TABLE quiz_attempts ADD PRIMARY KEY (id);
    END IF;

    -- TOPIC_PERFORMANCE
    IF performance_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'topic_performance' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE topic_performance DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
        END IF;
        ALTER TABLE topic_performance DROP COLUMN id;
        ALTER TABLE topic_performance RENAME COLUMN new_id TO id;
        ALTER TABLE topic_performance ADD PRIMARY KEY (id);
    END IF;

    -- CHAT_SESSIONS
    IF sessions_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'chat_sessions' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE chat_sessions DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
        END IF;
        ALTER TABLE chat_sessions DROP COLUMN id;
        ALTER TABLE chat_sessions RENAME COLUMN new_id TO id;
        ALTER TABLE chat_sessions ADD PRIMARY KEY (id);
    END IF;

    -- CHAT_MESSAGES
    IF messages_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'chat_messages' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
        END IF;
        ALTER TABLE chat_messages DROP COLUMN id;
        ALTER TABLE chat_messages RENAME COLUMN new_id TO id;
        ALTER TABLE chat_messages ADD PRIMARY KEY (id);
    END IF;

    -- LEARNING_PATHS
    IF paths_is_uuid THEN
        SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'learning_paths' AND constraint_type = 'PRIMARY KEY';
        IF pk_name IS NOT NULL THEN
            EXECUTE 'ALTER TABLE learning_paths DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
        END IF;
        ALTER TABLE learning_paths DROP COLUMN id;
        ALTER TABLE learning_paths RENAME COLUMN new_id TO id;
        ALTER TABLE learning_paths ADD PRIMARY KEY (id);
    END IF;

    -- -------------------------------------------------------------
    -- 8. PHASE 7: SWAP FOREIGN KEY COLUMNS
    -- -------------------------------------------------------------
    -- quiz_attempts: user_id, course_id, phase_id
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'new_user_id') THEN
        ALTER TABLE quiz_attempts DROP COLUMN user_id;
        ALTER TABLE quiz_attempts RENAME COLUMN new_user_id TO user_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'new_course_id') THEN
        ALTER TABLE quiz_attempts DROP COLUMN course_id;
        ALTER TABLE quiz_attempts RENAME COLUMN new_course_id TO course_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'new_phase_id') THEN
        ALTER TABLE quiz_attempts DROP COLUMN phase_id;
        ALTER TABLE quiz_attempts RENAME COLUMN new_phase_id TO phase_id;
    END IF;

    -- course_phases: course_id
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'new_course_id') THEN
        ALTER TABLE course_phases DROP COLUMN course_id;
        ALTER TABLE course_phases RENAME COLUMN new_course_id TO course_id;
    END IF;

    -- quiz_questions: course_id, phase_id
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'new_course_id') THEN
        ALTER TABLE quiz_questions DROP COLUMN course_id;
        ALTER TABLE quiz_questions RENAME COLUMN new_course_id TO course_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'new_phase_id') THEN
        ALTER TABLE quiz_questions DROP COLUMN phase_id;
        ALTER TABLE quiz_questions RENAME COLUMN new_phase_id TO phase_id;
    END IF;

    -- chat_sessions: user_id
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_sessions' AND column_name = 'new_user_id') THEN
        ALTER TABLE chat_sessions DROP COLUMN user_id;
        ALTER TABLE chat_sessions RENAME COLUMN new_user_id TO user_id;
    END IF;

    -- chat_messages: session_id
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'new_session_id') THEN
        ALTER TABLE chat_messages DROP COLUMN session_id;
        ALTER TABLE chat_messages RENAME COLUMN new_session_id TO session_id;
    END IF;

    -- topic_performance: user_id
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'new_user_id') THEN
        ALTER TABLE topic_performance DROP COLUMN user_id;
        ALTER TABLE topic_performance RENAME COLUMN new_user_id TO user_id;
    END IF;

    -- learning_paths: student_id
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'learning_paths' AND column_name = 'new_student_id') THEN
        ALTER TABLE learning_paths DROP COLUMN student_id;
        ALTER TABLE learning_paths RENAME COLUMN new_student_id TO student_id;
    END IF;

    -- courses: user_id
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'new_user_id') THEN
        ALTER TABLE courses DROP COLUMN user_id;
        ALTER TABLE courses RENAME COLUMN new_user_id TO user_id;
    END IF;

    -- -------------------------------------------------------------
    -- 9. PHASE 8: ENSURE ALL SPRING BOOT COLUMNS EXIST ACROSS ENTITIES
    -- -------------------------------------------------------------
    -- USERS
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
        ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(255);
        ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50) DEFAULT 'STUDENT';
        ALTER TABLE users ADD COLUMN IF NOT EXISTS branch VARCHAR(50);
        ALTER TABLE users ADD COLUMN IF NOT EXISTS academic_year VARCHAR(50);
        ALTER TABLE users ADD COLUMN IF NOT EXISTS semester VARCHAR(50);
        ALTER TABLE users ADD COLUMN IF NOT EXISTS cgpa NUMERIC(4,2) DEFAULT 0.00;
        ALTER TABLE users ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN DEFAULT FALSE;
        ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(50) DEFAULT 'EMAIL';
        ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
        ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

        UPDATE users SET username = split_part(email, '@', 1) WHERE username IS NULL OR username = '';
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'users_role_check') THEN
            ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (UPPER(role) IN ('STUDENT', 'FACULTY', 'ADMIN'));
        END IF;
    ELSE
        CREATE TABLE users (
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
            updated_at TIMESTAMPTZ DEFAULT NOW()
        );
    END IF;

    -- COURSES
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courses') THEN
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS user_email VARCHAR(255);
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS user_id BIGINT;
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS title VARCHAR(255) NOT NULL DEFAULT 'Course';
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS description TEXT;
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS difficulty VARCHAR(50) DEFAULT 'Standard';
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS progress INT DEFAULT 0;
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS current_phase INT DEFAULT 1;
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS total_phases INT DEFAULT 4;
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS is_custom BOOLEAN DEFAULT FALSE;
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
        ALTER TABLE courses ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
    ELSE
        CREATE TABLE courses (
            id BIGSERIAL PRIMARY KEY,
            user_id BIGINT,
            user_email VARCHAR(255) NOT NULL,
            title VARCHAR(255) NOT NULL,
            description TEXT,
            difficulty VARCHAR(50) DEFAULT 'Standard',
            progress INT DEFAULT 0,
            current_phase INT DEFAULT 1,
            total_phases INT DEFAULT 4,
            is_custom BOOLEAN DEFAULT FALSE,
            is_deleted BOOLEAN DEFAULT FALSE,
            deleted BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        );
    END IF;

    -- COURSE_PHASES
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'course_phases') THEN
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS course_id BIGINT;
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS phase_number INT DEFAULT 1;
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS title VARCHAR(255) DEFAULT 'Phase';
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS learning_objectives TEXT;
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS study_material TEXT DEFAULT '';
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS estimated_duration VARCHAR(50) DEFAULT '2-3 Hours';
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS is_unlocked BOOLEAN DEFAULT FALSE;
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS is_completed BOOLEAN DEFAULT FALSE;
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;
        ALTER TABLE course_phases ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
    ELSE
        CREATE TABLE course_phases (
            id BIGSERIAL PRIMARY KEY,
            course_id BIGINT,
            phase_number INT NOT NULL,
            title VARCHAR(255) NOT NULL,
            learning_objectives TEXT,
            study_material TEXT NOT NULL,
            estimated_duration VARCHAR(50) DEFAULT '2-3 Hours',
            is_unlocked BOOLEAN DEFAULT FALSE,
            is_completed BOOLEAN DEFAULT FALSE,
            completed_at TIMESTAMPTZ,
            created_at TIMESTAMPTZ DEFAULT NOW()
        );
    END IF;

    -- QUIZ_QUESTIONS
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quiz_questions') THEN
        ALTER TABLE quiz_questions ALTER COLUMN correct_option DROP DEFAULT;
        ALTER TABLE quiz_questions ALTER COLUMN correct_option TYPE VARCHAR(10) USING CASE WHEN correct_option::text = '0' THEN 'A' WHEN correct_option::text = '1' THEN 'B' WHEN correct_option::text = '2' THEN 'C' WHEN correct_option::text = '3' THEN 'D' ELSE UPPER(correct_option::text) END;
        ALTER TABLE quiz_questions ALTER COLUMN correct_option SET DEFAULT 'A';
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS phase_id BIGINT;
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS course_id BIGINT;
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS question_text TEXT DEFAULT '';
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS option_a TEXT DEFAULT '';
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS option_b TEXT DEFAULT '';
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS option_c TEXT DEFAULT '';
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS option_d TEXT DEFAULT '';
        ALTER TABLE quiz_questions ALTER COLUMN option_a DROP NOT NULL;
        ALTER TABLE quiz_questions ALTER COLUMN option_b DROP NOT NULL;
        ALTER TABLE quiz_questions ALTER COLUMN option_c DROP NOT NULL;
        ALTER TABLE quiz_questions ALTER COLUMN option_d DROP NOT NULL;
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS correct_option VARCHAR(10) DEFAULT 'A';
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS explanation TEXT DEFAULT '';
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS concept_tag VARCHAR(100);
        ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
    ELSE
        CREATE TABLE quiz_questions (
            id BIGSERIAL PRIMARY KEY,
            phase_id BIGINT,
            course_id BIGINT,
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
    END IF;

    -- QUIZ_ATTEMPTS
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quiz_attempts') THEN
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS user_id BIGINT;
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS course_id BIGINT;
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS phase_id BIGINT;
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS score INT DEFAULT 0;
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS total_questions INT DEFAULT 10;
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS percentage NUMERIC(5,2) DEFAULT 0.00;
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS passed BOOLEAN DEFAULT FALSE;
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS topic VARCHAR(255);
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS attempt_number INT DEFAULT 1;
        ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
    ELSE
        CREATE TABLE quiz_attempts (
            id BIGSERIAL PRIMARY KEY,
            user_id BIGINT,
            phase_id BIGINT,
            course_id BIGINT,
            score INT NOT NULL,
            total_questions INT NOT NULL DEFAULT 10,
            percentage NUMERIC(5,2) NOT NULL,
            passed BOOLEAN NOT NULL DEFAULT FALSE,
            topic VARCHAR(255),
            attempt_number INT DEFAULT 1,
            created_at TIMESTAMPTZ DEFAULT NOW()
        );
    END IF;

    -- TOPIC_PERFORMANCE
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'topic_performance') THEN
        ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS user_id BIGINT;
        ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS course_title VARCHAR(255) DEFAULT '';
        ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS concept_tag VARCHAR(100) DEFAULT '';
        ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS times_tested INT DEFAULT 1;
        ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS times_incorrect INT DEFAULT 0;
        ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'NEEDS_ATTENTION';
        ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS last_evaluated_at TIMESTAMPTZ DEFAULT NOW();
    ELSE
        CREATE TABLE topic_performance (
            id BIGSERIAL PRIMARY KEY,
            user_id BIGINT,
            course_title VARCHAR(255) NOT NULL,
            concept_tag VARCHAR(100) NOT NULL,
            times_tested INT DEFAULT 1,
            times_incorrect INT DEFAULT 0,
            status VARCHAR(50) DEFAULT 'NEEDS_ATTENTION',
            last_evaluated_at TIMESTAMPTZ DEFAULT NOW()
        );
    END IF;

    -- CHAT_SESSIONS
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_sessions') THEN
        ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS title VARCHAR(255) DEFAULT 'New Session';
        ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
        ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS context_type VARCHAR(50) DEFAULT 'STUDENT';
        ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS user_email VARCHAR(255);
        ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS user_id BIGINT;
        ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
        ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
    ELSE
        CREATE TABLE chat_sessions (
            id BIGSERIAL PRIMARY KEY,
            title VARCHAR(255) DEFAULT 'New Session',
            status VARCHAR(50) DEFAULT 'ACTIVE',
            context_type VARCHAR(50) DEFAULT 'STUDENT',
            user_email VARCHAR(255),
            user_id BIGINT,
            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        );
    END IF;

    -- CHAT_MESSAGES
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_messages') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'conversation_id') THEN
            ALTER TABLE chat_messages ALTER COLUMN conversation_id DROP NOT NULL;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'message') THEN
            ALTER TABLE chat_messages ALTER COLUMN message DROP NOT NULL;
        END IF;

        -- Drop any legacy check constraints on chat_messages (e.g. chat_messages_sender_check)
        FOR r IN (
            SELECT constraint_name
            FROM information_schema.table_constraints
            WHERE table_name = 'chat_messages' AND constraint_type = 'CHECK'
        ) LOOP
            EXECUTE 'ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
        END LOOP;

        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'id' AND data_type = 'uuid') THEN
            ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS new_id BIGSERIAL;
            FOR r IN (
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_name = 'chat_messages' AND constraint_type = 'PRIMARY KEY'
            ) LOOP
                EXECUTE 'ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name) || ' CASCADE';
            END LOOP;
            ALTER TABLE chat_messages DROP COLUMN IF EXISTS id CASCADE;
            ALTER TABLE chat_messages RENAME COLUMN new_id TO id;
            ALTER TABLE chat_messages ADD PRIMARY KEY (id);
        END IF;

        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS session_id BIGINT;
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS sender VARCHAR(50) DEFAULT 'user';
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS content TEXT DEFAULT '';
        ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS timestamp TIMESTAMPTZ DEFAULT NOW();
    ELSE
        CREATE TABLE chat_messages (
            id BIGSERIAL PRIMARY KEY,
            session_id BIGINT,
            sender VARCHAR(50) NOT NULL,
            content TEXT NOT NULL,
            timestamp TIMESTAMPTZ DEFAULT NOW()
        );
    END IF;

    -- EMAIL_OTPS
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'email_otps') THEN
        CREATE TABLE email_otps (
            id BIGSERIAL PRIMARY KEY,
            email VARCHAR(255) NOT NULL,
            otp_code VARCHAR(10) NOT NULL,
            purpose VARCHAR(50) DEFAULT 'VERIFICATION',
            attempts INT DEFAULT 0,
            is_used BOOLEAN DEFAULT FALSE,
            expires_at TIMESTAMPTZ NOT NULL,
            created_at TIMESTAMPTZ DEFAULT NOW()
        );
    ELSE
        ALTER TABLE email_otps ADD COLUMN IF NOT EXISTS purpose VARCHAR(50) DEFAULT 'VERIFICATION';
        ALTER TABLE email_otps ADD COLUMN IF NOT EXISTS attempts INT DEFAULT 0;
        ALTER TABLE email_otps ADD COLUMN IF NOT EXISTS is_used BOOLEAN DEFAULT FALSE;
    END IF;

    -- LEARNING_PATHS
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'learning_paths') THEN
        CREATE TABLE learning_paths (
            id BIGSERIAL PRIMARY KEY,
            student_id BIGINT,
            title VARCHAR(255),
            generated_content TEXT
        );
    END IF;

    -- SUGGESTED_COURSES
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'suggested_courses') THEN
        CREATE TABLE suggested_courses (
            id BIGSERIAL PRIMARY KEY,
            faculty_id BIGINT,
            faculty_name VARCHAR(255) NOT NULL,
            faculty_email VARCHAR(255) NOT NULL,
            title VARCHAR(255) NOT NULL,
            description TEXT,
            target_branch VARCHAR(50) NOT NULL,
            target_year VARCHAR(50),
            target_semester VARCHAR(50),
            priority VARCHAR(50) DEFAULT 'HIGH',
            deadline DATE,
            created_at TIMESTAMPTZ DEFAULT NOW()
        );
    ELSE
        ALTER TABLE suggested_courses ADD COLUMN IF NOT EXISTS target_year VARCHAR(50);
        ALTER TABLE suggested_courses ADD COLUMN IF NOT EXISTS target_semester VARCHAR(50);
        ALTER TABLE suggested_courses ADD COLUMN IF NOT EXISTS priority VARCHAR(50) DEFAULT 'HIGH';
        ALTER TABLE suggested_courses ADD COLUMN IF NOT EXISTS deadline DATE;
    END IF;

    -- -------------------------------------------------------------
    -- 10. PHASE 9: RE-APPLY ALL CLEAN BIGINT FOREIGN KEY CONSTRAINTS
    -- -------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_course_phases_course') THEN
        ALTER TABLE course_phases ADD CONSTRAINT fk_course_phases_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_quiz_questions_phase') THEN
        ALTER TABLE quiz_questions ADD CONSTRAINT fk_quiz_questions_phase FOREIGN KEY (phase_id) REFERENCES course_phases(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_quiz_questions_course') THEN
        ALTER TABLE quiz_questions ADD CONSTRAINT fk_quiz_questions_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_quiz_attempts_course') THEN
        ALTER TABLE quiz_attempts ADD CONSTRAINT fk_quiz_attempts_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_quiz_attempts_phase') THEN
        ALTER TABLE quiz_attempts ADD CONSTRAINT fk_quiz_attempts_phase FOREIGN KEY (phase_id) REFERENCES course_phases(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_quiz_attempts_user') THEN
        ALTER TABLE quiz_attempts ADD CONSTRAINT fk_quiz_attempts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_chat_sessions_user') THEN
        ALTER TABLE chat_sessions ADD CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_chat_messages_session') THEN
        ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_topic_performance_user') THEN
        ALTER TABLE topic_performance ADD CONSTRAINT fk_topic_performance_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_learning_paths_student') THEN
        ALTER TABLE learning_paths ADD CONSTRAINT fk_learning_paths_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;

    -- CLEAN NULL VALUES AND REMOVE NOT NULL ON LEGACY DUPLICATE COLUMNS
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'email_otps') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'email_otps' AND column_name = 'used') THEN
            ALTER TABLE email_otps ALTER COLUMN used DROP NOT NULL;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'email_otps' AND column_name = 'is_used') THEN
            ALTER TABLE email_otps ALTER COLUMN is_used DROP NOT NULL;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'email_otps' AND column_name = 'used') AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'email_otps' AND column_name = 'is_used') THEN
            UPDATE email_otps SET is_used = COALESCE(is_used, used, FALSE), used = COALESCE(used, is_used, FALSE);
        ELSE
            UPDATE email_otps SET is_used = FALSE WHERE is_used IS NULL;
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courses') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'deleted') THEN
            ALTER TABLE courses ALTER COLUMN deleted DROP NOT NULL;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'is_deleted') THEN
            ALTER TABLE courses ALTER COLUMN is_deleted DROP NOT NULL;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'custom') THEN
            ALTER TABLE courses ALTER COLUMN custom DROP NOT NULL;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'is_custom') THEN
            ALTER TABLE courses ALTER COLUMN is_custom DROP NOT NULL;
        END IF;
        UPDATE courses SET is_custom = FALSE WHERE is_custom IS NULL;
        UPDATE courses SET is_deleted = FALSE WHERE is_deleted IS NULL;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'course_phases') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'unlocked') THEN
            ALTER TABLE course_phases ALTER COLUMN unlocked DROP NOT NULL;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'completed') THEN
            ALTER TABLE course_phases ALTER COLUMN completed DROP NOT NULL;
        END IF;
        UPDATE course_phases SET is_unlocked = FALSE WHERE is_unlocked IS NULL;
        UPDATE course_phases SET is_completed = FALSE WHERE is_completed IS NULL;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_messages') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'conversation_id') THEN
            ALTER TABLE chat_messages ALTER COLUMN conversation_id DROP NOT NULL;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'message') THEN
            ALTER TABLE chat_messages ALTER COLUMN message DROP NOT NULL;
        END IF;

        -- Drop any legacy check constraints on chat_messages (e.g. chat_messages_sender_check)
        FOR r IN (
            SELECT constraint_name
            FROM information_schema.table_constraints
            WHERE table_name = 'chat_messages' AND constraint_type = 'CHECK'
        ) LOOP
            EXECUTE 'ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
        END LOOP;
    END IF;

    -- 11. SEED DEFAULT TEST ACCOUNTS FOR END-TO-END VERIFICATION (IF ABSENT)
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        INSERT INTO users (email, username, role, branch, academic_year, semester, cgpa, is_email_verified, auth_provider, password)
        SELECT '22vr1a0501@vrsec.ac.in', 'Student 22VR1A0501', 'STUDENT', 'CSE', '2nd Year', '1st Sem', 8.50, TRUE, 'EMAIL', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f'
        WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = '22vr1a0501@vrsec.ac.in');

        INSERT INTO users (email, username, role, branch, academic_year, semester, cgpa, is_email_verified, auth_provider, password)
        SELECT 'hod.cse@siddhartha.ac.in', 'Dr. HOD CSE', 'FACULTY', 'CSE', NULL, NULL, NULL, TRUE, 'EMAIL', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f'
        WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'hod.cse@siddhartha.ac.in');
    END IF;

    -- -------------------------------------------------------------
    -- 11. PHASE 10: CREATE PERFORMANCE & AUDIT INDEXES
    -- -------------------------------------------------------------
    CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
    CREATE INDEX IF NOT EXISTS idx_courses_user_email ON courses(user_email);
    CREATE INDEX IF NOT EXISTS idx_course_phases_course ON course_phases(course_id);
    CREATE INDEX IF NOT EXISTS idx_quiz_questions_phase ON quiz_questions(phase_id);
    CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user ON quiz_attempts(user_id);
    CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_id);
    CREATE INDEX IF NOT EXISTS idx_topic_performance_user ON topic_performance(user_id);

    RAISE NOTICE 'LearnPath AI relational UUID to BIGINT migration completed successfully with full relational integrity preserved.';
END $$;
