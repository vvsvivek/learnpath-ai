package com.vrsec.backend.controller;

import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import com.vrsec.backend.service.AuthService;
import com.vrsec.backend.service.LearningPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/main")
@CrossOrigin(origins = "*")
@Transactional(readOnly = true)
public class ApiController {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private TopicPerformanceRepository topicPerformanceRepository;
    @Autowired private LearningPathService learningPathService;
    @Autowired private AuthService authService;

    // =========================================================================
    // 1. AUTH COMPATIBILITY ENDPOINTS (For legacy clients calling /api/main/auth)
    // =========================================================================
    @PostMapping({"/auth/send-otp", "/send-otp"})
    public ResponseEntity<?> legacySendOtp(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            return ResponseEntity.ok(authService.sendOtp(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping({"/auth", "/auth/verify-otp"})
    public ResponseEntity<?> legacyVerifyOtp(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            String otp = req.get("otp");
            String username = req.get("username");
            String password = req.get("password");
            return ResponseEntity.ok(authService.verifyOtp(email, otp, username, password));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // =========================================================================
    // 2. STUDENT DASHBOARD (Real DB records, Dynamic Recommendations)
    // =========================================================================
    @GetMapping({"/dashboard", "/student/dashboard"})
    public ResponseEntity<?> getDashboard(@RequestParam String email) {
        try {
            String cleanEmail = email != null ? email.trim().toLowerCase() : "";
            User student = userRepository.findByEmail(cleanEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + cleanEmail));

            List<Course> allCourses = courseRepository.findByUserEmailIgnoreCase(cleanEmail);
            List<Course> activeCourses = allCourses.stream().filter(c -> !c.isDeleted()).collect(Collectors.toList());
            List<Course> deletedCourses = allCourses.stream().filter(Course::isDeleted).collect(Collectors.toList());

            List<QuizAttempt> userAttempts = quizAttemptRepository.findByUserIdOrderByCreatedAtDesc(student.getId());
            List<TopicPerformance> weakTopics = topicPerformanceRepository.findByUserIdAndStatus(student.getId(), "NEEDS_ATTENTION");

            // Format active courses with phase details
            List<Map<String, Object>> formattedCourses = new ArrayList<>();
            for (Course c : activeCourses) {
                Map<String, Object> cmap = new HashMap<>();
                cmap.put("id", c.getId());
                cmap.put("title", c.getTitle());
                cmap.put("description", c.getDescription());
                cmap.put("progress", c.getProgress());
                cmap.put("currentPhase", c.getCurrentPhase());
                cmap.put("totalPhases", c.getTotalPhases());
                cmap.put("difficulty", c.getDifficulty());

                List<CoursePhase> phases = phaseRepository.findByCourseIdOrderByPhaseNumberAsc(c.getId());
                List<Map<String, Object>> pList = new ArrayList<>();
                for (CoursePhase p : phases) {
                    Map<String, Object> pmap = new HashMap<>();
                    pmap.put("id", p.getId());
                    pmap.put("phaseNumber", p.getPhaseNumber());
                    pmap.put("title", p.getTitle());
                    pmap.put("objectives", p.getLearningObjectives());
                    pmap.put("duration", p.getEstimatedDuration());
                    pmap.put("isUnlocked", p.isUnlocked());
                    pmap.put("isCompleted", p.isCompleted());
                    pList.add(pmap);
                }
                cmap.put("phases", pList);
                formattedCourses.add(cmap);
            }

            // Generate personalized recommendations
            List<Map<String, Object>> recommendations = learningPathService.getPersonalizedRecommendations(student);

            Map<String, Object> response = new HashMap<>();
            response.put("profile", authService.sanitizeUser(student));
            response.put("activeCourses", formattedCourses);
            response.put("deletedCourses", deletedCourses);
            response.put("recommendations", recommendations);
            response.put("weakTopics", weakTopics);
            response.put("totalCompletedCourses", activeCourses.stream().filter(c -> c.getProgress() != null && c.getProgress() == 100).count());
            response.put("cgpa", student.getCgpa() != null ? student.getCgpa() : 0.0);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Error loading dashboard: " + e.getMessage()));
        }
    }

    // =========================================================================
    // 3. COURSE SELECTION & EXPLORE CUSTOM COURSE (DYNAMIC LEARNING PATHS)
    // =========================================================================
    @PostMapping({"/course/select", "/generate-path", "/course/enroll"})
    public ResponseEntity<?> selectOrGenerateCourse(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            String subject = req.get("subject");
            if (subject == null) subject = req.get("courseTitle");
            if (subject == null) subject = req.get("title");

            if (email == null || subject == null || subject.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email and subject are required."));
            }

            User student = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found."));

            Course course = learningPathService.generateSubjectLearningPath(student, subject.trim());

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Learning path generated for " + course.getTitle());
            res.put("courseId", course.getId());
            res.put("title", course.getTitle());
            res.put("totalPhases", course.getTotalPhases());
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to generate learning path: " + e.getMessage()));
        }
    }

    @GetMapping("/phase/details")
    public ResponseEntity<?> getPhaseDetails(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String subject,
            @RequestParam(defaultValue = "1") Integer phaseNumber,
            @RequestParam(required = false) String email) {
        try {
            CoursePhase targetPhase = null;
            if (courseId != null) {
                targetPhase = phaseRepository.findByCourseIdAndPhaseNumber(courseId, phaseNumber).orElse(null);
            }
            if (targetPhase == null && subject != null) {
                List<Course> courses = courseRepository.findAll().stream()
                        .filter(c -> c.getTitle().equalsIgnoreCase(subject.trim()) && !c.isDeleted())
                        .toList();
                if (!courses.isEmpty()) {
                    targetPhase = phaseRepository.findByCourseIdAndPhaseNumber(courses.get(0).getId(), phaseNumber).orElse(null);
                }
            }

            if (targetPhase == null) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", "Phase details not found."));
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", targetPhase.getId());
            map.put("courseId", targetPhase.getCourse().getId());
            map.put("courseTitle", targetPhase.getCourse().getTitle());
            map.put("phaseNumber", targetPhase.getPhaseNumber());
            map.put("title", targetPhase.getTitle());
            map.put("objectives", targetPhase.getLearningObjectives());
            map.put("studyMaterial", targetPhase.getStudyMaterial());
            map.put("duration", targetPhase.getEstimatedDuration());
            map.put("isUnlocked", targetPhase.isUnlocked());
            map.put("isCompleted", targetPhase.isCompleted());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @Transactional
    @PostMapping("/course/toggle-delete")
    public ResponseEntity<?> toggleCourseDelete(@RequestBody Map<String, Object> req) {
        try {
            Long courseId = Long.valueOf(req.get("courseId").toString());
            boolean isDeleted = req.containsKey("isDeleted") ? (Boolean) req.get("isDeleted") : true;
            Optional<Course> opt = courseRepository.findById(courseId);
            if (opt.isPresent()) {
                Course c = opt.get();
                c.setDeleted(isDeleted);
                courseRepository.save(c);
                return ResponseEntity.ok(Collections.singletonMap("message", "Course visibility updated"));
            }
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // =========================================================================
    // 4. PROFILE UPDATE
    // =========================================================================
    @Transactional
    @PostMapping("/profile/update")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> req) {
        try {
            String email = (String) req.get("email");
            if (email == null) return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email is required."));
            User user = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            if (req.get("username") != null) user.setUsername(req.get("username").toString().trim());
            if (req.get("branch") != null) user.setBranch(req.get("branch").toString().trim());
            if (req.get("academicYear") != null) user.setAcademicYear(req.get("academicYear").toString().trim());
            if (req.get("year") != null) user.setAcademicYear(req.get("year").toString().trim());
            if (req.get("semester") != null) user.setSemester(req.get("semester").toString().trim());
            if (req.get("sem") != null) user.setSemester(req.get("sem").toString().trim());
            if (req.get("cgpa") != null) {
                try { user.setCgpa(Double.parseDouble(req.get("cgpa").toString().trim())); } catch (Exception ignored) {}
            }

            userRepository.save(user);
            return ResponseEntity.ok(authService.sanitizeUser(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
