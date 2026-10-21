package com.vrsec.backend.controller;

import com.vrsec.backend.model.Course;
import com.vrsec.backend.model.QuizAttempt;
import com.vrsec.backend.repository.CourseRepository;
import com.vrsec.backend.repository.QuizAttemptRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v2/faculty")
@CrossOrigin(origins = "*")
public class FacultyController {

    private final CourseRepository courseRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public FacultyController(CourseRepository courseRepository, QuizAttemptRepository quizAttemptRepository) {
        this.courseRepository = courseRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    @GetMapping("/overall-progress")
    public ResponseEntity<?> getFacultyAnalytics(@RequestParam String branch) {
        List<Course> allCourses = courseRepository.findAll();
        List<QuizAttempt> allQuizzes = quizAttemptRepository.findAll();

        int completed = 0, inProgress = 0, notStarted = 0;
        for (Course c : allCourses) {
            if (c.getProgress() != null && c.getProgress() == 100) completed++;
            else if (c.getProgress() != null && c.getProgress() > 0) inProgress++;
            else notStarted++;
        }

        int high = 0, medium = 0, low = 0;
        for (QuizAttempt q : allQuizzes) {
            if (q.getTotalQuestions() == null || q.getTotalQuestions() == 0) continue;
            double pct = ((double) q.getScore() / q.getTotalQuestions()) * 100;
            if (pct >= 75) high++;
            else if (pct >= 50) medium++;
            else low++;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalCompleted", completed);
        
        response.put("learningStatus", Arrays.asList(
            Map.of("name", "Completed", "population", completed, "color", "#10B981", "legendFontColor", "#1E293B"),
            Map.of("name", "In Progress", "population", inProgress, "color", "#F59E0B", "legendFontColor", "#1E293B"),
            Map.of("name", "Not Started", "population", notStarted, "color", "#94A3B8", "legendFontColor", "#1E293B")
        ));

        response.put("assessmentPerformance", Arrays.asList(
            Map.of("name", "75% and above", "population", high, "color", "#3B82F6", "legendFontColor", "#1E293B"),
            Map.of("name", "50% - 74%", "population", medium, "color", "#8B5CF6", "legendFontColor", "#1E293B"),
            Map.of("name", "Below 50%", "population", low, "color", "#EF4444", "legendFontColor", "#1E293B")
        ));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cohorts")
    public ResponseEntity<?> getCohorts(@RequestParam String branch) {
        return ResponseEntity.ok(Arrays.asList(
            Map.of("cohortName", "Semester 5 (Section A)", "count", 45),
            Map.of("cohortName", "Semester 5 (Section B)", "count", 42)
        ));
    }

    @GetMapping("/cohort-stats")
    public ResponseEntity<?> getCohortStats(@RequestParam String cohortName, @RequestParam String branch) {
        return ResponseEntity.ok(Map.of("totalStudents", 45));
    }

    @PostMapping("/recommend")
    public ResponseEntity<?> recommendCourse(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(Map.of("message", "Recommended successfully"));
    }
}