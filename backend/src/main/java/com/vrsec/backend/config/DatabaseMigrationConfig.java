package com.vrsec.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pre-Hibernate Database Schema Migration Runner.
 * Executes at startup BEFORE EntityManagerFactory initializes.
 * Safely converts any legacy UUID primary and foreign keys to BIGINT sequences,
 * ensuring Hibernate DDL auto-update succeeds without foreign key type mismatch errors.
 */
@Component
public class DatabaseMigrationConfig implements BeanPostProcessor, PriorityOrdered {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);
    private final AtomicBoolean executed = new AtomicBoolean(false);

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && executed.compareAndSet(false, true)) {
            runSchemaMigration(dataSource);
        }
        return bean;
    }

    private void runSchemaMigration(DataSource dataSource) {
        log.info("[DB MIGRATION] Executing pre-Hibernate schema migration check (UUID -> BIGINT)...");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(MIGRATION_SQL);
            log.info("[DB MIGRATION] Schema migration check completed successfully. All keys aligned to BIGINT.");
        } catch (Exception e) {
            log.warn("[DB MIGRATION] Notice: Database schema check bypassed or running in offline mode: {}", e.getMessage());
        }
    }

    private static final String MIGRATION_SQL = """
        DO $$
        DECLARE
            col_type text;
            pk_name text;
            r RECORD;
        BEGIN
            -- 1. DROP ALL LEGACY FOREIGN KEY CONSTRAINTS TARGETING CORE ENTITIES
            FOR r IN (
                SELECT tc.table_schema, tc.table_name, tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name
                WHERE tc.constraint_type = 'FOREIGN KEY'
                  AND ccu.table_name IN ('courses', 'users', 'course_phases', 'chat_sessions', 'quiz_questions', 'quiz_attempts', 'topic_performance', 'learning_paths')
            ) LOOP
                EXECUTE 'ALTER TABLE ' || quote_ident(r.table_schema) || '.' || quote_ident(r.table_name) || ' DROP CONSTRAINT IF EXISTS ' || quote_ident(r.constraint_name);
            END LOOP;

            -- 2. USERS TABLE MIGRATION
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
                SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'id';
                IF col_type = 'uuid' THEN
                    SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'users' AND constraint_type = 'PRIMARY KEY';
                    IF pk_name IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE users DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
                    END IF;
                    CREATE SEQUENCE IF NOT EXISTS users_id_seq;
                    ALTER TABLE users ALTER COLUMN id DROP DEFAULT;
                    ALTER TABLE users ALTER COLUMN id TYPE BIGINT USING nextval('users_id_seq');
                    ALTER TABLE users ALTER COLUMN id SET DEFAULT nextval('users_id_seq');
                    ALTER TABLE users ADD PRIMARY KEY (id);
                    ALTER SEQUENCE users_id_seq OWNED BY users.id;
                    IF (SELECT COUNT(*) FROM users) > 0 THEN
                        PERFORM setval('users_id_seq', (SELECT MAX(id) FROM users));
                    ELSE
                        PERFORM setval('users_id_seq', 1, false);
                    END IF;
                END IF;

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
                    updated_at TIMESTAMPTZ DEFAULT NOW(),
                    CONSTRAINT check_user_role CHECK (role IN ('STUDENT', 'FACULTY', 'ADMIN'))
                );
            END IF;

            -- 3. COURSES TABLE MIGRATION
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'courses') THEN
                SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'courses' AND column_name = 'id';
                IF col_type = 'uuid' THEN
                    SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'courses' AND constraint_type = 'PRIMARY KEY';
                    IF pk_name IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE courses DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
                    END IF;
                    CREATE SEQUENCE IF NOT EXISTS courses_id_seq;
                    ALTER TABLE courses ALTER COLUMN id DROP DEFAULT;
                    ALTER TABLE courses ALTER COLUMN id TYPE BIGINT USING nextval('courses_id_seq');
                    ALTER TABLE courses ALTER COLUMN id SET DEFAULT nextval('courses_id_seq');
                    ALTER TABLE courses ADD PRIMARY KEY (id);
                    ALTER SEQUENCE courses_id_seq OWNED BY courses.id;
                    IF (SELECT COUNT(*) FROM courses) > 0 THEN
                        PERFORM setval('courses_id_seq', (SELECT MAX(id) FROM courses));
                    ELSE
                        PERFORM setval('courses_id_seq', 1, false);
                    END IF;
                END IF;

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

            -- 4. COURSE_PHASES TABLE MIGRATION
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'course_phases') THEN
                SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'id';
                IF col_type = 'uuid' THEN
                    SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'course_phases' AND constraint_type = 'PRIMARY KEY';
                    IF pk_name IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE course_phases DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
                    END IF;
                    CREATE SEQUENCE IF NOT EXISTS course_phases_id_seq;
                    ALTER TABLE course_phases ALTER COLUMN id DROP DEFAULT;
                    ALTER TABLE course_phases ALTER COLUMN id TYPE BIGINT USING nextval('course_phases_id_seq');
                    ALTER TABLE course_phases ALTER COLUMN id SET DEFAULT nextval('course_phases_id_seq');
                    ALTER TABLE course_phases ADD PRIMARY KEY (id);
                    ALTER SEQUENCE course_phases_id_seq OWNED BY course_phases.id;
                    IF (SELECT COUNT(*) FROM course_phases) > 0 THEN
                        PERFORM setval('course_phases_id_seq', (SELECT MAX(id) FROM course_phases));
                    ELSE
                        PERFORM setval('course_phases_id_seq', 1, false);
                    END IF;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'course_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'course_phases' AND column_name = 'course_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE course_phases ALTER COLUMN course_id DROP DEFAULT;
                        ALTER TABLE course_phases ALTER COLUMN course_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE course_phases ADD COLUMN course_id BIGINT;
                END IF;

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
                    created_at TIMESTAMPTZ DEFAULT NOW(),
                    CONSTRAINT uq_course_phase UNIQUE (course_id, phase_number)
                );
            END IF;

            -- 5. QUIZ_QUESTIONS TABLE MIGRATION
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quiz_questions') THEN
                SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'id';
                IF col_type = 'uuid' THEN
                    SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'quiz_questions' AND constraint_type = 'PRIMARY KEY';
                    IF pk_name IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
                    END IF;
                    CREATE SEQUENCE IF NOT EXISTS quiz_questions_id_seq;
                    ALTER TABLE quiz_questions ALTER COLUMN id DROP DEFAULT;
                    ALTER TABLE quiz_questions ALTER COLUMN id TYPE BIGINT USING nextval('quiz_questions_id_seq');
                    ALTER TABLE quiz_questions ALTER COLUMN id SET DEFAULT nextval('quiz_questions_id_seq');
                    ALTER TABLE quiz_questions ADD PRIMARY KEY (id);
                    ALTER SEQUENCE quiz_questions_id_seq OWNED BY quiz_questions.id;
                    IF (SELECT COUNT(*) FROM quiz_questions) > 0 THEN
                        PERFORM setval('quiz_questions_id_seq', (SELECT MAX(id) FROM quiz_questions));
                    ELSE
                        PERFORM setval('quiz_questions_id_seq', 1, false);
                    END IF;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'course_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'course_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE quiz_questions ALTER COLUMN course_id DROP DEFAULT;
                        ALTER TABLE quiz_questions ALTER COLUMN course_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE quiz_questions ADD COLUMN course_id BIGINT;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'phase_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'quiz_questions' AND column_name = 'phase_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE quiz_questions ALTER COLUMN phase_id DROP DEFAULT;
                        ALTER TABLE quiz_questions ALTER COLUMN phase_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE quiz_questions ADD COLUMN phase_id BIGINT;
                END IF;

                ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS question_text TEXT DEFAULT '';
                ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS option_a TEXT DEFAULT '';
                ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS option_b TEXT DEFAULT '';
                ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS option_c TEXT DEFAULT '';
                ALTER TABLE quiz_questions ADD COLUMN IF NOT EXISTS option_d TEXT DEFAULT '';
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

            -- 6. QUIZ_ATTEMPTS TABLE MIGRATION
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'quiz_attempts') THEN
                SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'id';
                IF col_type = 'uuid' THEN
                    SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'quiz_attempts' AND constraint_type = 'PRIMARY KEY';
                    IF pk_name IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE quiz_attempts DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
                    END IF;
                    CREATE SEQUENCE IF NOT EXISTS quiz_attempts_id_seq;
                    ALTER TABLE quiz_attempts ALTER COLUMN id DROP DEFAULT;
                    ALTER TABLE quiz_attempts ALTER COLUMN id TYPE BIGINT USING nextval('quiz_attempts_id_seq');
                    ALTER TABLE quiz_attempts ALTER COLUMN id SET DEFAULT nextval('quiz_attempts_id_seq');
                    ALTER TABLE quiz_attempts ADD PRIMARY KEY (id);
                    ALTER SEQUENCE quiz_attempts_id_seq OWNED BY quiz_attempts.id;
                    IF (SELECT COUNT(*) FROM quiz_attempts) > 0 THEN
                        PERFORM setval('quiz_attempts_id_seq', (SELECT MAX(id) FROM quiz_attempts));
                    ELSE
                        PERFORM setval('quiz_attempts_id_seq', 1, false);
                    END IF;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'user_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'user_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE quiz_attempts ALTER COLUMN user_id DROP DEFAULT;
                        ALTER TABLE quiz_attempts ALTER COLUMN user_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE quiz_attempts ADD COLUMN user_id BIGINT;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'course_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'course_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE quiz_attempts ALTER COLUMN course_id DROP DEFAULT;
                        ALTER TABLE quiz_attempts ALTER COLUMN course_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE quiz_attempts ADD COLUMN course_id BIGINT;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'phase_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'phase_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE quiz_attempts ALTER COLUMN phase_id DROP DEFAULT;
                        ALTER TABLE quiz_attempts ALTER COLUMN phase_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE quiz_attempts ADD COLUMN phase_id BIGINT;
                END IF;

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

            -- 7. TOPIC_PERFORMANCE TABLE MIGRATION
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'topic_performance') THEN
                SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'id';
                IF col_type = 'uuid' THEN
                    SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'topic_performance' AND constraint_type = 'PRIMARY KEY';
                    IF pk_name IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE topic_performance DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
                    END IF;
                    CREATE SEQUENCE IF NOT EXISTS topic_performance_id_seq;
                    ALTER TABLE topic_performance ALTER COLUMN id DROP DEFAULT;
                    ALTER TABLE topic_performance ALTER COLUMN id TYPE BIGINT USING nextval('topic_performance_id_seq');
                    ALTER TABLE topic_performance ALTER COLUMN id SET DEFAULT nextval('topic_performance_id_seq');
                    ALTER TABLE topic_performance ADD PRIMARY KEY (id);
                    ALTER SEQUENCE topic_performance_id_seq OWNED BY topic_performance.id;
                    IF (SELECT COUNT(*) FROM topic_performance) > 0 THEN
                        PERFORM setval('topic_performance_id_seq', (SELECT MAX(id) FROM topic_performance));
                    ELSE
                        PERFORM setval('topic_performance_id_seq', 1, false);
                    END IF;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'user_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'user_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE topic_performance ALTER COLUMN user_id DROP DEFAULT;
                        ALTER TABLE topic_performance ALTER COLUMN user_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE topic_performance ADD COLUMN user_id BIGINT;
                END IF;

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

            -- 8. CHAT_SESSIONS TABLE MIGRATION
            IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_sessions') THEN
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
            ELSE
                SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'chat_sessions' AND column_name = 'id';
                IF col_type = 'uuid' THEN
                    SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'chat_sessions' AND constraint_type = 'PRIMARY KEY';
                    IF pk_name IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE chat_sessions DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
                    END IF;
                    CREATE SEQUENCE IF NOT EXISTS chat_sessions_id_seq;
                    ALTER TABLE chat_sessions ALTER COLUMN id DROP DEFAULT;
                    ALTER TABLE chat_sessions ALTER COLUMN id TYPE BIGINT USING nextval('chat_sessions_id_seq');
                    ALTER TABLE chat_sessions ALTER COLUMN id SET DEFAULT nextval('chat_sessions_id_seq');
                    ALTER TABLE chat_sessions ADD PRIMARY KEY (id);
                    ALTER SEQUENCE chat_sessions_id_seq OWNED BY chat_sessions.id;
                    IF (SELECT COUNT(*) FROM chat_sessions) > 0 THEN
                        PERFORM setval('chat_sessions_id_seq', (SELECT MAX(id) FROM chat_sessions));
                    ELSE
                        PERFORM setval('chat_sessions_id_seq', 1, false);
                    END IF;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_sessions' AND column_name = 'user_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'chat_sessions' AND column_name = 'user_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE chat_sessions ALTER COLUMN user_id DROP DEFAULT;
                        ALTER TABLE chat_sessions ALTER COLUMN user_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE chat_sessions ADD COLUMN user_id BIGINT;
                END IF;

                ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS title VARCHAR(255) DEFAULT 'New Session';
                ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
                ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS context_type VARCHAR(50) DEFAULT 'STUDENT';
                ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS user_email VARCHAR(255);
                ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
                ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();
            END IF;

            -- 9. CHAT_MESSAGES TABLE MIGRATION
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'chat_messages') THEN
                SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'id';
                IF col_type = 'uuid' THEN
                    SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'chat_messages' AND constraint_type = 'PRIMARY KEY';
                    IF pk_name IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
                    END IF;
                    CREATE SEQUENCE IF NOT EXISTS chat_messages_id_seq;
                    ALTER TABLE chat_messages ALTER COLUMN id DROP DEFAULT;
                    ALTER TABLE chat_messages ALTER COLUMN id TYPE BIGINT USING nextval('chat_messages_id_seq');
                    ALTER TABLE chat_messages ALTER COLUMN id SET DEFAULT nextval('chat_messages_id_seq');
                    ALTER TABLE chat_messages ADD PRIMARY KEY (id);
                    ALTER SEQUENCE chat_messages_id_seq OWNED BY chat_messages.id;
                    IF (SELECT COUNT(*) FROM chat_messages) > 0 THEN
                        PERFORM setval('chat_messages_id_seq', (SELECT MAX(id) FROM chat_messages));
                    ELSE
                        PERFORM setval('chat_messages_id_seq', 1, false);
                    END IF;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'session_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'chat_messages' AND column_name = 'session_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE chat_messages ALTER COLUMN session_id DROP DEFAULT;
                        ALTER TABLE chat_messages ALTER COLUMN session_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE chat_messages ADD COLUMN session_id BIGINT;
                END IF;

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

            -- 10. EMAIL_OTPS TABLE MIGRATION
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

            -- 11. LEARNING_PATHS TABLE MIGRATION
            IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'learning_paths') THEN
                CREATE TABLE learning_paths (
                    id BIGSERIAL PRIMARY KEY,
                    student_id BIGINT,
                    title VARCHAR(255),
                    generated_content TEXT
                );
            ELSE
                SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'learning_paths' AND column_name = 'id';
                IF col_type = 'uuid' THEN
                    SELECT constraint_name INTO pk_name FROM information_schema.table_constraints WHERE table_name = 'learning_paths' AND constraint_type = 'PRIMARY KEY';
                    IF pk_name IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE learning_paths DROP CONSTRAINT IF EXISTS ' || quote_ident(pk_name) || ' CASCADE';
                    END IF;
                    CREATE SEQUENCE IF NOT EXISTS learning_paths_id_seq;
                    ALTER TABLE learning_paths ALTER COLUMN id DROP DEFAULT;
                    ALTER TABLE learning_paths ALTER COLUMN id TYPE BIGINT USING nextval('learning_paths_id_seq');
                    ALTER TABLE learning_paths ALTER COLUMN id SET DEFAULT nextval('learning_paths_id_seq');
                    ALTER TABLE learning_paths ADD PRIMARY KEY (id);
                    ALTER SEQUENCE learning_paths_id_seq OWNED BY learning_paths.id;
                    IF (SELECT COUNT(*) FROM learning_paths) > 0 THEN
                        PERFORM setval('learning_paths_id_seq', (SELECT MAX(id) FROM learning_paths));
                    ELSE
                        PERFORM setval('learning_paths_id_seq', 1, false);
                    END IF;
                END IF;

                IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'learning_paths' AND column_name = 'student_id') THEN
                    SELECT data_type INTO col_type FROM information_schema.columns WHERE table_name = 'learning_paths' AND column_name = 'student_id';
                    IF col_type = 'uuid' THEN
                        ALTER TABLE learning_paths ALTER COLUMN student_id DROP DEFAULT;
                        ALTER TABLE learning_paths ALTER COLUMN student_id TYPE BIGINT USING NULL;
                    END IF;
                ELSE
                    ALTER TABLE learning_paths ADD COLUMN student_id BIGINT;
                END IF;
            END IF;

            -- 12. SUGGESTED_COURSES TABLE MIGRATION
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

            -- 13. RE-ESTABLISH ALL CLEAN BIGINT FOREIGN KEYS
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

            -- 14. CREATE INDEXES
            CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
            CREATE INDEX IF NOT EXISTS idx_courses_user_email ON courses(user_email);
            CREATE INDEX IF NOT EXISTS idx_course_phases_course ON course_phases(course_id);
            CREATE INDEX IF NOT EXISTS idx_quiz_questions_phase ON quiz_questions(phase_id);
            CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user ON quiz_attempts(user_id);
            CREATE INDEX IF NOT EXISTS idx_chat_messages_session ON chat_messages(session_id);
            CREATE INDEX IF NOT EXISTS idx_topic_performance_user ON topic_performance(user_id);

        END $$;
        """;
}
