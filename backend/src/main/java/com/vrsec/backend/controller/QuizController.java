package com.vrsec.backend.controller;

import com.vrsec.backend.model.Course;
import com.vrsec.backend.model.CoursePhase;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.CoursePhaseRepository;
import com.vrsec.backend.repository.CourseRepository;
import com.vrsec.backend.repository.UserRepository;
import com.vrsec.backend.service.LearningPathService;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired private DataSource dataSource;

    /**
     * Inspects and returns current Supabase quiz_questions column types and constraints,
     * safely aligning correct_option to VARCHAR(10) if not already migrated.
     */
    @GetMapping("/schema")
    public ResponseEntity<?> getQuizSchema() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            List<Map<String, Object>> columns = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT column_name, data_type, column_default, is_nullable " +
                    "FROM information_schema.columns WHERE table_name = 'quiz_questions' ORDER BY ordinal_position")) {
                while (rs.next()) {
                    Map<String, Object> col = new LinkedHashMap<>();
                    col.put("column", rs.getString("column_name"));
                    col.put("type", rs.getString("data_type"));
                    col.put("default", rs.getString("column_default"));
                    col.put("nullable", rs.getString("is_nullable"));
                    columns.add(col);
                }
            }
            result.put("columns", columns);

            List<Map<String, Object>> constraints = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT conname, pg_get_constraintdef(c.oid) as def " +
                    "FROM pg_constraint c JOIN pg_class t ON c.conrelid = t.oid " +
                    "WHERE t.relname = 'quiz_questions'")) {
                while (rs.next()) {
                    Map<String, Object> con = new LinkedHashMap<>();
                    con.put("name", rs.getString("conname"));
                    con.put("definition", rs.getString("def"));
                    constraints.add(con);
                }
            }
            result.put("constraints", constraints);

            boolean needsFix = columns.stream().anyMatch(c -> "correct_option".equals(c.get("column")) && !"character varying".equals(c.get("type")));
            result.put("needsFix", needsFix);
            if (needsFix) {
                conn.setAutoCommit(true);
                List<String> fixLogs = new ArrayList<>();
                String[] fixStatements = {
                    "ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS check_correct_option",
                    "ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS quiz_questions_correct_option_check",
                    "ALTER TABLE quiz_questions ALTER COLUMN correct_option DROP DEFAULT",
                    "ALTER TABLE quiz_questions ALTER COLUMN correct_option TYPE VARCHAR(10) USING CASE WHEN correct_option::text = '0' THEN 'A' WHEN correct_option::text = '1' THEN 'B' WHEN correct_option::text = '2' THEN 'C' WHEN correct_option::text = '3' THEN 'D' ELSE UPPER(correct_option::text) END",
                    "ALTER TABLE quiz_questions ALTER COLUMN correct_option SET DEFAULT 'A'"
                };
                for (String sql : fixStatements) {
                    try {
                        stmt.execute(sql);
                        fixLogs.add("SUCCESS: " + sql);
                    } catch (Exception ex) {
                        fixLogs.add("FAILED (" + sql + "): " + ex.getMessage());
                    }
                }
                result.put("fixLogs", fixLogs);
            }

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
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody Map<String, Object> req) {
        try {
            String email = (String) req.get("email");
            if (email == null) return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email is required."));
            User student = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found."));

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
