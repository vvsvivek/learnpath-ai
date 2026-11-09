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

import java.util.*;

@RestController
@RequestMapping("/api/main/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    @Autowired private LearningPathService learningPathService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private UserRepository userRepository;

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
