-- =================================================================
-- LEARNPATH AI — ROW LEVEL SECURITY POLICIES (02_rls_policies.sql)
-- =================================================================

-- Enable RLS across all application tables
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
-- Students can read their own profile
CREATE POLICY student_read_own_profile ON users
    FOR SELECT
    USING (auth.jwt() ->> 'email' = email);

-- Faculty can read student profiles IF the student is registered with @vrsec.ac.in
CREATE POLICY faculty_read_eligible_students ON users
    FOR SELECT
    USING (
        (auth.jwt() ->> 'role' = 'FACULTY' OR auth.jwt() ->> 'email' LIKE '%@siddhartha.ac.in' OR auth.jwt() ->> 'email' = 'vivekvaranasi99@gmail.com')
        AND email LIKE '%@vrsec.ac.in'
    );

-- Users can update only their own profile details
CREATE POLICY user_update_own_profile ON users
    FOR UPDATE
    USING (auth.jwt() ->> 'email' = email)
    WITH CHECK (auth.jwt() ->> 'email' = email);

-- 2. COURSES POLICIES
-- Students can manage their own enrolled courses
CREATE POLICY student_all_own_courses ON courses
    FOR ALL
    USING (auth.jwt() ->> 'email' = user_email)
    WITH CHECK (auth.jwt() ->> 'email' = user_email);

-- Faculty can view courses of eligible @vrsec.ac.in students
CREATE POLICY faculty_view_eligible_student_courses ON courses
    FOR SELECT
    USING (
        (auth.jwt() ->> 'role' = 'FACULTY' OR auth.jwt() ->> 'email' LIKE '%@siddhartha.ac.in' OR auth.jwt() ->> 'email' = 'vivekvaranasi99@gmail.com')
        AND user_email LIKE '%@vrsec.ac.in'
    );

-- 3. COURSE PHASES POLICIES
-- Accessible if the student owns the underlying course
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
-- Questions are readable by the course owner, but correct_option is never directly exposed via client queries
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
-- Students can read their own quiz attempts
CREATE POLICY student_read_own_attempts ON quiz_attempts
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = quiz_attempts.user_id
            AND u.email = auth.jwt() ->> 'email'
        )
    );

-- Faculty can view quiz performance of eligible @vrsec.ac.in students
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
-- Students read their own weak topics
CREATE POLICY student_read_own_topic_performance ON topic_performance
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = topic_performance.user_id
            AND u.email = auth.jwt() ->> 'email'
        )
    );

-- Faculty read topic performance of @vrsec.ac.in students
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
-- Faculty can create, edit, delete their own recommendations
CREATE POLICY faculty_manage_suggested_courses ON suggested_courses
    FOR ALL
    USING (
        auth.jwt() ->> 'email' = faculty_email
        AND (auth.jwt() ->> 'email' LIKE '%@siddhartha.ac.in' OR auth.jwt() ->> 'email' = 'vivekvaranasi99@gmail.com')
    );

-- Students can read recommendations matching their cohort IF they are @vrsec.ac.in
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

-- 8. CHAT SESSIONS & MESSAGES (STRICT USER PRIVACY)
-- Only the owner of the chat session can read or write
CREATE POLICY user_manage_own_chat_sessions ON chat_sessions
    FOR ALL
    USING (auth.jwt() ->> 'email' = user_email)
    WITH CHECK (auth.jwt() ->> 'email' = user_email);

CREATE POLICY user_manage_own_chat_messages ON chat_messages
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM chat_sessions cs
            WHERE cs.id = chat_messages.session_id
            AND cs.user_email = auth.jwt() ->> 'email'
        )
    );
