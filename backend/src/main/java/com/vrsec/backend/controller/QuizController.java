package com.vrsec.backend.controller;

import com.vrsec.backend.model.Course;
import com.vrsec.backend.model.CoursePhase;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.CoursePhaseRepository;
import com.vrsec.backend.repository.CourseRepository;
import com.vrsec.backend.repository.UserRepository;
import com.vrsec.backend.service.JwtService;
import com.vrsec.backend.service.LearningPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

@RestController
@RequestMapping("/api/main/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    @Autowired private LearningPathService learningPathService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private DataSource dataSource;

    /**
     * Inspects and returns current Supabase quiz table column types and constraints,
     * safely aligning quiz_questions, quiz_attempts, and topic_performance schemas.
     */
    @GetMapping("/schema")
    public ResponseEntity<?> getQuizSchema() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
            String[] targetTables = {"quiz_questions", "quiz_attempts", "topic_performance"};
            for (String tbl : targetTables) {
                List<Map<String, Object>> cols = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT column_name, data_type, column_default, is_nullable " +
                        "FROM information_schema.columns WHERE table_name = '" + tbl + "' ORDER BY ordinal_position")) {
                    while (rs.next()) {
                        Map<String, Object> col = new LinkedHashMap<>();
                        col.put("column", rs.getString("column_name"));
                        col.put("type", rs.getString("data_type"));
                        col.put("default", rs.getString("column_default"));
                        col.put("nullable", rs.getString("is_nullable"));
                        cols.add(col);
                    }
                }
                tables.put(tbl, cols);
            }
            result.put("tables", tables);

            List<Map<String, Object>> constraints = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT c.conname, pg_get_constraintdef(c.oid) as def, t.relname " +
                    "FROM pg_constraint c JOIN pg_class t ON c.conrelid = t.oid " +
                    "WHERE t.relname IN ('quiz_questions', 'quiz_attempts', 'topic_performance')")) {
                while (rs.next()) {
                    Map<String, Object> con = new LinkedHashMap<>();
                    con.put("table", rs.getString("relname"));
                    con.put("name", rs.getString("conname"));
                    con.put("definition", rs.getString("def"));
                    constraints.add(con);
                }
            }
            result.put("constraints", constraints);

            List<String> fixLogs = new ArrayList<>();
            conn.setAutoCommit(true);

            String[] legacyCleanups = {
                // quiz_questions cleanups
                "ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS check_correct_option",
                "ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS quiz_questions_correct_option_check",
                "ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS quiz_questions_quiz_id_fkey",
                "ALTER TABLE quiz_questions ALTER COLUMN quiz_id DROP NOT NULL",
                "ALTER TABLE quiz_questions ALTER COLUMN options DROP NOT NULL",
                "ALTER TABLE quiz_questions ALTER COLUMN optiona DROP NOT NULL",
                "ALTER TABLE quiz_questions ALTER COLUMN optionb DROP NOT NULL",
                "ALTER TABLE quiz_questions ALTER COLUMN optionc DROP NOT NULL",
                "ALTER TABLE quiz_questions ALTER COLUMN optiond DROP NOT NULL",
                "ALTER TABLE quiz_questions ALTER COLUMN correct_option DROP DEFAULT",
                "ALTER TABLE quiz_questions ALTER COLUMN correct_option TYPE VARCHAR(10) USING CASE WHEN correct_option::text = '0' THEN 'A' WHEN correct_option::text = '1' THEN 'B' WHEN correct_option::text = '2' THEN 'C' WHEN correct_option::text = '3' THEN 'D' ELSE UPPER(correct_option::text) END",
                "ALTER TABLE quiz_questions ALTER COLUMN correct_option SET DEFAULT 'A'",

                // quiz_attempts cleanups
                "ALTER TABLE quiz_attempts DROP CONSTRAINT IF EXISTS quiz_attempts_quiz_id_fkey",
                "ALTER TABLE quiz_attempts DROP CONSTRAINT IF EXISTS check_quiz_score",
                "ALTER TABLE quiz_attempts DROP CONSTRAINT IF EXISTS quiz_attempts_score_check",
                "ALTER TABLE quiz_attempts ALTER COLUMN quiz_id DROP NOT NULL",
                "ALTER TABLE quiz_attempts ALTER COLUMN time_taken DROP NOT NULL",
                "ALTER TABLE quiz_attempts ALTER COLUMN completed_at DROP NOT NULL",
                "ALTER TABLE quiz_attempts ALTER COLUMN total_questions DROP NOT NULL",
                "ALTER TABLE quiz_attempts ALTER COLUMN score DROP NOT NULL",
                "ALTER TABLE quiz_attempts ALTER COLUMN percentage DROP NOT NULL",
                "ALTER TABLE quiz_attempts ALTER COLUMN passed DROP NOT NULL",
                "ALTER TABLE quiz_attempts ALTER COLUMN topic DROP NOT NULL",
                "ALTER TABLE quiz_attempts ALTER COLUMN attempt_number DROP NOT NULL",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS user_id BIGINT",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS course_id BIGINT",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS phase_id BIGINT",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS score INT DEFAULT 0",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS total_questions INT DEFAULT 10",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS percentage NUMERIC(5,2) DEFAULT 0.00",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS passed BOOLEAN DEFAULT FALSE",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS topic VARCHAR(255)",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS attempt_number INT DEFAULT 1",
                "ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW()",

                // topic_performance cleanups
                "CREATE TABLE IF NOT EXISTS topic_performance (id BIGSERIAL PRIMARY KEY, user_id BIGINT, course_title VARCHAR(255) NOT NULL DEFAULT '', concept_tag VARCHAR(100) NOT NULL DEFAULT '', times_tested INT DEFAULT 1, times_incorrect INT DEFAULT 0, status VARCHAR(50) DEFAULT 'NEEDS_ATTENTION', last_evaluated_at TIMESTAMPTZ DEFAULT NOW())",
                "ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS user_id BIGINT",
                "ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS course_title VARCHAR(255) DEFAULT ''",
                "ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS concept_tag VARCHAR(100) DEFAULT ''",
                "ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS times_tested INT DEFAULT 1",
                "ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS times_incorrect INT DEFAULT 0",
                "ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'NEEDS_ATTENTION'",
                "ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS last_evaluated_at TIMESTAMPTZ DEFAULT NOW()",
                "ALTER TABLE topic_performance ALTER COLUMN topic DROP NOT NULL",
                "ALTER TABLE topic_performance ALTER COLUMN accuracy DROP NOT NULL",
                "ALTER TABLE topic_performance DROP CONSTRAINT IF EXISTS topic_performance_topic_id_fkey",
                "ALTER TABLE topic_performance ALTER COLUMN topic_id DROP NOT NULL",

                // Idempotent PL/pgSQL block to ensure ID and foreign keys are BIGINT
                "DO $$ BEGIN " +
                "  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'id' AND data_type = 'uuid') THEN " +
                "    ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; " +
                "    ALTER TABLE quiz_attempts DROP CONSTRAINT IF EXISTS quiz_attempts_pkey CASCADE; " +
                "    ALTER TABLE quiz_attempts DROP COLUMN IF EXISTS id CASCADE; " +
                "    ALTER TABLE quiz_attempts RENAME COLUMN new_id TO id; " +
                "    ALTER TABLE quiz_attempts ADD PRIMARY KEY (id); " +
                "  END IF; " +
                "  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'user_id' AND data_type = 'uuid') THEN " +
                "    ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS new_user_id BIGINT; " +
                "    ALTER TABLE quiz_attempts DROP COLUMN IF EXISTS user_id CASCADE; " +
                "    ALTER TABLE quiz_attempts RENAME COLUMN new_user_id TO user_id; " +
                "  END IF; " +
                "  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'course_id' AND data_type = 'uuid') THEN " +
                "    ALTER TABLE quiz_attempts DROP COLUMN IF EXISTS course_id CASCADE; " +
                "    ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS course_id BIGINT; " +
                "  END IF; " +
                "  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quiz_attempts' AND column_name = 'phase_id' AND data_type = 'uuid') THEN " +
                "    ALTER TABLE quiz_attempts DROP COLUMN IF EXISTS phase_id CASCADE; " +
                "    ALTER TABLE quiz_attempts ADD COLUMN IF NOT EXISTS phase_id BIGINT; " +
                "  END IF; " +
                "  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'id' AND data_type = 'uuid') THEN " +
                "    ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS new_id BIGSERIAL; " +
                "    ALTER TABLE topic_performance DROP CONSTRAINT IF EXISTS topic_performance_pkey CASCADE; " +
                "    ALTER TABLE topic_performance DROP COLUMN IF EXISTS id CASCADE; " +
                "    ALTER TABLE topic_performance RENAME COLUMN new_id TO id; " +
                "    ALTER TABLE topic_performance ADD PRIMARY KEY (id); " +
                "  END IF; " +
                "  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'topic_performance' AND column_name = 'user_id' AND data_type = 'uuid') THEN " +
                "    ALTER TABLE topic_performance ADD COLUMN IF NOT EXISTS new_user_id BIGINT; " +
                "    ALTER TABLE topic_performance DROP COLUMN IF EXISTS user_id CASCADE; " +
                "    ALTER TABLE topic_performance RENAME COLUMN new_user_id TO user_id; " +
                "  END IF; " +
                "END $$;"
            };

            for (String sql : legacyCleanups) {
                try {
                    stmt.execute(sql);
                    fixLogs.add("SUCCESS: " + sql);
                } catch (Exception ex) {
                    fixLogs.add("NOTE (" + sql + "): " + ex.getMessage());
                }
            }
            result.put("fixLogs", fixLogs);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Generates or fetches EXACTLY 10 multiple-choice questions for the specified phase.
     * Crucial security requirement: Correct answers are stored on the backend and NOT returned to client.
     */
    @GetMapping("/generate")
    public ResponseEntity<?> generateQuiz(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "1") Integer phaseNumber) {
        try {
            Long targetCourseId = courseId;
            if (targetCourseId == null && subject != null) {
                List<Course> matches = courseRepository.findAll().stream()
                        .filter(c -> c.getTitle().equalsIgnoreCase(subject.trim()) && !c.isDeleted())
                        .toList();
                if (!matches.isEmpty()) {
                    targetCourseId = matches.get(0).getId();
                }
            }

            if (targetCourseId == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Course identifier is required."));
            }

            List<Map<String, Object>> questions = learningPathService.getOrCreatePhaseQuiz(targetCourseId, phaseNumber);
            Map<String, Object> response = new HashMap<>();
            response.put("courseId", targetCourseId);
            response.put("phaseNumber", phaseNumber);
            response.put("totalQuestions", questions.size());
            response.put("questions", questions);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Error generating quiz: " + e.getMessage()));
        }
    }

    // Alias for legacy mobile screens calling /api/main/quiz/generate-dynamic or /dynamic
    @GetMapping({"/generate-dynamic", "/dynamic"})
    public ResponseEntity<?> generateDynamicQuizLegacy(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "1") Integer phaseNumber) {
        return generateQuiz(null, course != null ? course : topic, phaseNumber);
    }

    /**
     * Submits quiz answers for server-side evaluation.
     * Enforces the STRICT > 60% rule: score / 10 > 0.60 (7 or more correct answers required).
     * SECURITY: Requires verified student JWT. Enforces that caller owns the course.
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> req) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Authorization header. A valid bearer token is required to submit a quiz."));
            }

            String token = authHeader.substring(7).trim();
            Map<String, Object> claims = jwtService.validateAndExtractClaims(token);
            if (claims == null || claims.get("sub") == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("error",
                        "Authentication failed: Invalid or expired authentication token."));
            }

            String authEmail = claims.get("sub").toString().trim().toLowerCase();
            User student = userRepository.findByEmail(authEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated student not found: " + authEmail));

            if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error",
                        "Access denied: Quizzes can only be submitted by students."));
            }

            Long courseId = null;
            if (req.get("courseId") != null) {
                courseId = Long.valueOf(req.get("courseId").toString());
            } else if (req.get("subject") != null || req.get("courseTitle") != null) {
                String title = (String) (req.get("subject") != null ? req.get("subject") : req.get("courseTitle"));
                courseId = courseRepository.findByUserEmailIgnoreCaseAndDeletedFalse(student.getEmail()).stream()
                        .filter(c -> c.getTitle().equalsIgnoreCase(title.trim()))
                        .map(Course::getId).findFirst().orElse(null);
            }

            if (courseId == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Course ID not found."));
            }

            // Verify course ownership
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found."));
            if (course.getUserEmail() != null && !course.getUserEmail().equalsIgnoreCase(student.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error",
                        "Access denied: You do not own this course. Cross-student quiz submissions are strictly forbidden."));
            }

            Integer phaseNumber = req.get("phaseNumber") != null ? Integer.valueOf(req.get("phaseNumber").toString()) : 1;

            // Extract answers map { questionId: selectedOption }
            Map<Long, String> answersMap = new HashMap<>();
            if (req.containsKey("answers") && req.get("answers") instanceof Map) {
                Map<?, ?> raw = (Map<?, ?>) req.get("answers");
                for (Map.Entry<?, ?> entry : raw.entrySet()) {
                    try {
                        Long qId = Long.valueOf(entry.getKey().toString());
                        String opt = entry.getValue() != null ? entry.getValue().toString() : "";
                        answersMap.put(qId, opt);
                    } catch (Exception ignored) {}
                }
            }

            Map<String, Object> result = learningPathService.submitQuizAnswers(student, courseId, phaseNumber, answersMap);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Submission error: " + e.getMessage()));
        }
    }
}
