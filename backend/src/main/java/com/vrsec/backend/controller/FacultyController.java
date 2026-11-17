package com.vrsec.backend.controller;

import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import com.vrsec.backend.service.JwtService;
import com.vrsec.backend.service.LearningPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/faculty", "/api/v2/faculty", "/api/main/faculty"})
@CrossOrigin(origins = "*")
@Transactional(readOnly = true)
public class FacultyController {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private TopicPerformanceRepository topicPerformanceRepository;
    @Autowired private SuggestedCourseRepository suggestedCourseRepository;
    @Autowired private JwtService jwtService;

    /**
     * Resolves authenticated faculty caller identity and department from JWT token.
     */
    private User resolveFacultyCaller(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7).trim();
            Map<String, Object> claims = jwtService.validateAndExtractClaims(token);
            if (claims == null || claims.get("sub") == null) {
                return null;
            }
            String email = claims.get("sub").toString().trim().toLowerCase();
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Overall Faculty Dashboard Analytics strictly for verified @vrsec.ac.in students.
     * Enforces Department Isolation: Faculty accounts only receive analytics for their own department.
     */
    @GetMapping({"/overview", "/overall-progress"})
    public ResponseEntity<?> getFacultyOverview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String branch) {
        try {
            User caller = resolveFacultyCaller(authHeader);
            String effectiveBranch = branch;

            // Strict department isolation: if caller is a faculty member with an assigned branch, lock to their department
            if (caller != null && "FACULTY".equalsIgnoreCase(caller.getRole()) && caller.getBranch() != null && !caller.getBranch().trim().isEmpty()) {
                effectiveBranch = LearningPathService.normalizeBranch(caller.getBranch());
            } else if (effectiveBranch != null && !effectiveBranch.trim().isEmpty() && !"ALL".equalsIgnoreCase(effectiveBranch)) {
                effectiveBranch = LearningPathService.normalizeBranch(effectiveBranch);
            }

            // Filter ONLY eligible @vrsec.ac.in students
            List<User> students = userRepository.findAll().stream()
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                    .collect(Collectors.toList());

            if (effectiveBranch != null && !effectiveBranch.trim().isEmpty() && !"ALL".equalsIgnoreCase(effectiveBranch)) {
                final String targetBranch = effectiveBranch;
                students = students.stream()
                        .filter(u -> u.getBranch() != null && LearningPathService.normalizeBranch(u.getBranch()).equalsIgnoreCase(targetBranch))
                        .collect(Collectors.toList());
            }

            List<String> studentEmails = students.stream().map(User::getEmail).collect(Collectors.toList());
            List<Course> courses = courseRepository.findByUserEmailIn(studentEmails).stream()
                    .filter(c -> !c.isDeleted())
                    .collect(Collectors.toList());
            List<QuizAttempt> attempts = quizAttemptRepository.findAll().stream()
                    .filter(a -> a.getUser() != null && studentEmails.contains(a.getUser().getEmail()))
                    .collect(Collectors.toList());

            // 1. Branch Distribution
            Map<String, Long> branchDistribution = students.stream()
                    .collect(Collectors.groupingBy(u -> u.getBranch() != null ? LearningPathService.normalizeBranch(u.getBranch()) : "GENERAL", Collectors.counting()));

            // 2. Year Distribution
            Map<String, Long> yearDistribution = students.stream()
                    .collect(Collectors.groupingBy(u -> u.getAcademicYear() != null ? u.getAcademicYear() : "1st Year", Collectors.counting()));

            // 3. Semester Distribution
            Map<String, Long> semDistribution = students.stream()
                    .collect(Collectors.groupingBy(u -> u.getSemester() != null ? u.getSemester() : "1st Sem", Collectors.counting()));

            // 4. Learning Status Distribution
            long completedCourses = courses.stream().filter(c -> c.getProgress() != null && c.getProgress() == 100).count();
            long inProgressCourses = courses.stream().filter(c -> c.getProgress() != null && c.getProgress() > 0 && c.getProgress() < 100).count();
            long notStartedCourses = courses.stream().filter(c -> c.getProgress() == null || c.getProgress() == 0).count();

            // 5. Quiz Performance Breakdown
            long highPerformers = attempts.stream().filter(a -> a.getPercentage() != null && a.getPercentage() >= 75.0).count();
            long midPerformers = attempts.stream().filter(a -> a.getPercentage() != null && a.getPercentage() >= 60.0 && a.getPercentage() < 75.0).count();
            long lowPerformers = attempts.stream().filter(a -> a.getPercentage() != null && a.getPercentage() < 60.0).count();

            // 6. Cohort Classification: Slow Learners, Fast Learners, Consistent Learners
            List<Map<String, Object>> studentsNeedingSupport = new ArrayList<>();
            long slowLearnersCount = 0;
            long fastLearnersCount = 0;
            long consistentLearnersCount = 0;

            for (User s : students) {
                List<QuizAttempt> sAttempts = attempts.stream()
                        .filter(a -> a.getUser() != null && a.getUser().getId().equals(s.getId()))
                        .collect(Collectors.toList());
                long failedCount = sAttempts.stream().filter(a -> a.getPassed() != null && !a.getPassed()).count();
                boolean lowCgpa = s.getCgpa() != null && s.getCgpa() > 0 && s.getCgpa() < 6.5;
                
                List<Course> sCourses = courses.stream()
                        .filter(c -> c.getUserEmail() != null && c.getUserEmail().equalsIgnoreCase(s.getEmail()))
                        .collect(Collectors.toList());
                double sAvgProgress = sCourses.isEmpty() ? 0.0 :
                        sCourses.stream().mapToInt(c -> c.getProgress() != null ? c.getProgress() : 0).average().orElse(0.0);

                boolean isSlow = lowCgpa || failedCount >= 1 || (!sCourses.isEmpty() && sAvgProgress < 35.0);
                boolean isFast = (s.getCgpa() != null && s.getCgpa() >= 8.0) || sAvgProgress >= 75.0;

                if (isSlow) {
                    slowLearnersCount++;
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", s.getId());
                    item.put("username", s.getUsername());
                    item.put("email", s.getEmail());
                    item.put("branch", s.getBranch());
                    item.put("academicYear", s.getAcademicYear());
                    item.put("semester", s.getSemester());
                    item.put("cgpa", s.getCgpa());
                    item.put("failedQuizzesCount", failedCount);
                    item.put("averageProgress", Math.round(sAvgProgress * 10.0) / 10.0);
                    item.put("reason", lowCgpa ? "CGPA " + s.getCgpa() + " below 6.5 benchmark" : 
                            (failedCount >= 1 ? failedCount + " failed quiz assessment(s)" : "Low completion progress (" + (int)sAvgProgress + "%)"));
                    item.put("status", "Requires Additional Support");
                    studentsNeedingSupport.add(item);
                } else if (isFast) {
                    fastLearnersCount++;
                } else {
                    consistentLearnersCount++;
                }
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("totalEligibleStudents", students.size());
            resp.put("activeLearners", courses.size());
            resp.put("completedCourses", completedCourses);
            resp.put("inProgressCourses", inProgressCourses);
            resp.put("notStartedCourses", notStartedCourses);
            resp.put("branchDistribution", branchDistribution);
            resp.put("yearDistribution", yearDistribution);
            resp.put("semesterDistribution", semDistribution);
            
            // Cohort Breakdown
            resp.put("slowLearnersCount", slowLearnersCount);
            resp.put("fastLearnersCount", fastLearnersCount);
            resp.put("consistentLearnersCount", consistentLearnersCount);
            resp.put("studentsNeedingSupportCount", slowLearnersCount);
            resp.put("studentsNeedingSupport", studentsNeedingSupport);
            resp.put("effectiveDepartment", effectiveBranch != null ? effectiveBranch : "ALL");
            resp.put("isDepartmentLocked", caller != null && "FACULTY".equalsIgnoreCase(caller.getRole()));

            // Chart data arrays for UI charts
            resp.put("learningStatus", Arrays.asList(
                Map.of("name", "Completed", "population", completedCourses, "color", "#10B981", "legendFontColor", "#1E293B"),
                Map.of("name", "In Progress", "population", inProgressCourses, "color", "#4A90D9", "legendFontColor", "#1E293B"),
                Map.of("name", "Not Started", "population", notStartedCourses, "color", "#94A3B8", "legendFontColor", "#1E293B")
            ));

            resp.put("assessmentPerformance", Arrays.asList(
                Map.of("name", "75% and Above", "population", highPerformers, "color", "#1E3A5F", "legendFontColor", "#1E293B"),
                Map.of("name", "60% - 74%", "population", midPerformers, "color", "#4A90D9", "legendFontColor", "#1E293B"),
                Map.of("name", "Below 60%", "population", lowPerformers, "color", "#EF4444", "legendFontColor", "#1E293B")
            ));

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Error loading faculty overview: " + e.getMessage()));
        }
    }

    /**
     * Lists eligible @vrsec.ac.in students with multi-parameter filtering:
     * - Department Isolation: locked strictly to faculty's department
     * - Year-wise filter (1st Year, 2nd Year, 3rd Year, 4th Year)
     * - Semester-wise filter (1st Sem, 2nd Sem)
     * - Cohort Category (SLOW_LEARNER, FAST_LEARNER, CONSISTENT, ALL)
     */
    @GetMapping("/students")
    public ResponseEntity<?> getStudents(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        try {
            User caller = resolveFacultyCaller(authHeader);
            String effectiveBranch = branch;

            // Strict department isolation: if caller is a faculty member with an assigned branch, lock to their department
            if (caller != null && "FACULTY".equalsIgnoreCase(caller.getRole()) && caller.getBranch() != null && !caller.getBranch().trim().isEmpty()) {
                effectiveBranch = LearningPathService.normalizeBranch(caller.getBranch());
            } else if (effectiveBranch != null && !effectiveBranch.trim().isEmpty() && !"ALL".equalsIgnoreCase(effectiveBranch)) {
                effectiveBranch = LearningPathService.normalizeBranch(effectiveBranch);
            }

            List<User> students = userRepository.findAll().stream()
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                    .collect(Collectors.toList());

            if (effectiveBranch != null && !effectiveBranch.trim().isEmpty() && !"ALL".equalsIgnoreCase(effectiveBranch)) {
                final String targetBranch = effectiveBranch;
                students = students.stream()
                        .filter(u -> u.getBranch() != null && LearningPathService.normalizeBranch(u.getBranch()).equalsIgnoreCase(targetBranch))
                        .collect(Collectors.toList());
            }
            if (year != null && !year.trim().isEmpty() && !"ALL".equalsIgnoreCase(year)) {
                String y = year.trim().toLowerCase();
                students = students.stream().filter(u -> u.getAcademicYear() != null && u.getAcademicYear().toLowerCase().contains(y)).collect(Collectors.toList());
            }
            if (semester != null && !semester.trim().isEmpty() && !"ALL".equalsIgnoreCase(semester)) {
                String s = semester.trim().toLowerCase();
                students = students.stream().filter(u -> u.getSemester() != null && u.getSemester().toLowerCase().contains(s)).collect(Collectors.toList());
            }
            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                students = students.stream().filter(u -> 
                    (u.getUsername() != null && u.getUsername().toLowerCase().contains(q)) ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                ).collect(Collectors.toList());
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (User u : students) {
                List<Course> userCourses = courseRepository.findByUserEmailIgnoreCaseAndDeletedFalse(u.getEmail());
                double avgProgress = userCourses.isEmpty() ? 0.0 : 
                    userCourses.stream().mapToInt(c -> c.getProgress() != null ? c.getProgress() : 0).average().orElse(0.0);
                avgProgress = Math.round(avgProgress * 10.0) / 10.0;

                List<QuizAttempt> userAttempts = quizAttemptRepository.findByUserIdOrderByCreatedAtDesc(u.getId());
                long failedCount = userAttempts.stream().filter(a -> a.getPassed() != null && !a.getPassed()).count();

                Double cgpa = u.getCgpa();
                boolean isSlow = (cgpa != null && cgpa > 0.0 && cgpa < 6.5) || failedCount >= 1 || (!userCourses.isEmpty() && avgProgress < 35.0);
                boolean isFast = (cgpa != null && cgpa >= 8.0) || avgProgress >= 75.0;

                String learnerCategory;
                String categoryLabel;
                String reason;
                if (isSlow) {
                    learnerCategory = "SLOW_LEARNER";
                    categoryLabel = "⚠️ Needs Support";
                    if (failedCount >= 1 && cgpa != null && cgpa < 6.5) {
                        reason = "Low CGPA (" + cgpa + ") & " + failedCount + " failed quiz(zes)";
                    } else if (failedCount >= 1) {
                        reason = failedCount + " failed quiz assessment(s) requiring remediation";
                    } else if (cgpa != null && cgpa < 6.5) {
                        reason = "CGPA " + cgpa + " below 6.50 benchmark";
                    } else {
                        reason = "Low course progress (" + (int) avgProgress + "%)";
                    }
                } else if (isFast) {
                    learnerCategory = "FAST_LEARNER";
                    categoryLabel = "⭐ High Achiever";
                    reason = cgpa != null && cgpa >= 8.5 ? "Exceptional CGPA (" + cgpa + ") & fast mastery" : "Strong progression & high assessment scores";
                } else {
                    learnerCategory = "CONSISTENT";
                    categoryLabel = "📘 Consistent";
                    reason = "Steady progress across enrolled syllabus";
                }

                // Filter by Category if provided
                if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                    String cat = category.trim().toUpperCase();
                    if (cat.contains("SLOW") && !isSlow) continue;
                    if (cat.contains("FAST") && !isFast) continue;
                    if (cat.contains("CONSISTENT") && (isSlow || isFast)) continue;
                }

                // Filter by Legacy status if provided
                if ("NEEDS_ATTENTION".equalsIgnoreCase(status) && !isSlow) {
                    continue;
                }

                Map<String, Object> map = new HashMap<>();
                map.put("id", u.getId());
                map.put("name", u.getUsername());
                map.put("username", u.getUsername());
                map.put("email", u.getEmail());
                map.put("branch", u.getBranch());
                map.put("year", u.getAcademicYear());
                map.put("academicYear", u.getAcademicYear());
                map.put("semester", u.getSemester());
                map.put("cgpa", u.getCgpa());
                map.put("enrolledCoursesCount", userCourses.size());
                map.put("averageProgress", avgProgress);
                map.put("failedQuizzesCount", failedCount);
                map.put("category", learnerCategory);
                map.put("categoryLabel", categoryLabel);
                map.put("performanceStatus", isSlow ? "Needs Attention" : (isFast ? "High Achiever" : "Good Standing"));
                map.put("supportReason", reason);

                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Comprehensive Individual Student Report for faculty inspection.
     * Enforces Department Isolation: Faculty cannot inspect students belonging to another department.
     */
    @GetMapping("/student/{id}")
    public ResponseEntity<?> getIndividualStudentReport(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        try {
            User student = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));

            if (!student.getEmail().endsWith("@vrsec.ac.in")) {
                return ResponseEntity.status(403).body(Collections.singletonMap("error", "Faculty can only inspect records of verified @vrsec.ac.in students."));
            }

            // Department Isolation Check
            User caller = resolveFacultyCaller(authHeader);
            if (caller != null && "FACULTY".equalsIgnoreCase(caller.getRole()) && caller.getBranch() != null && !caller.getBranch().trim().isEmpty()) {
                String facultyBranch = LearningPathService.normalizeBranch(caller.getBranch());
                String studentBranch = LearningPathService.normalizeBranch(student.getBranch());
                if (!facultyBranch.equalsIgnoreCase(studentBranch)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("error",
                            "Access Restricted: You are assigned to the " + facultyBranch + " department and cannot view records of " + studentBranch + " students."));
                }
            }

            List<Course> courses = courseRepository.findByUserEmailIgnoreCaseAndDeletedFalse(student.getEmail());
            List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByCreatedAtDesc(student.getId());
            List<TopicPerformance> weakTopics = topicPerformanceRepository.findByUserId(student.getId());

            Map<String, Object> report = new HashMap<>();
            report.put("profile", student);
            report.put("enrolledCourses", courses);
            report.put("quizAttempts", attempts);
            report.put("topicPerformance", weakTopics);

            double avgScore = attempts.isEmpty() ? 0.0 : attempts.stream().mapToDouble(QuizAttempt::getPercentage).average().orElse(0.0);
            report.put("overallAssessmentAverage", Math.round(avgScore * 10.0) / 10.0);

            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Faculty assigns a recommended course to a target cohort.
     * Enforces eligibility: only eligible @vrsec.ac.in students matching the targeted branch, year, semester receive it.
     */
    @Transactional
    @PostMapping({"/recommend", "/suggest-course"})
    public ResponseEntity<?> recommendCourse(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> req) {
        try {
            User caller = resolveFacultyCaller(authHeader);
            String title = (String) (req.get("title") != null ? req.get("title") : req.get("courseTitle"));
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Course title is required."));
            }

            String defaultBranch = "CSE";
            String defaultFacultyName = "Department Faculty";
            String defaultFacultyEmail = "faculty@siddhartha.ac.in";

            if (caller != null) {
                if (caller.getBranch() != null && !caller.getBranch().trim().isEmpty()) {
                    defaultBranch = LearningPathService.normalizeBranch(caller.getBranch());
                }
                if (caller.getUsername() != null) defaultFacultyName = caller.getUsername();
                if (caller.getEmail() != null) defaultFacultyEmail = caller.getEmail();
            }

            SuggestedCourse sc = new SuggestedCourse();
            sc.setTitle(title.trim());
            sc.setDescription((String) req.getOrDefault("description", "Recommended by Department Faculty"));
            sc.setTargetBranch((String) req.getOrDefault("branch", defaultBranch));
            sc.setTargetYear((String) req.getOrDefault("year", "2nd Year"));
            sc.setTargetSemester((String) req.getOrDefault("semester", "1st Sem"));
            sc.setFacultyName((String) req.getOrDefault("facultyName", defaultFacultyName));
            sc.setFacultyEmail((String) req.getOrDefault("facultyEmail", defaultFacultyEmail));
            sc.setPriority((String) req.getOrDefault("priority", "HIGH"));

            if (req.containsKey("deadline") && req.get("deadline") != null) {
                try { sc.setDeadline(LocalDate.parse(req.get("deadline").toString())); } catch (Exception ignored) {}
            }

            suggestedCourseRepository.save(sc);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Course successfully published to all @" + sc.getTargetBranch() + " (" + sc.getTargetYear() + " - " + sc.getTargetSemester() + ") eligible students."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to assign course: " + e.getMessage()));
        }
    }

    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(@RequestParam(required = false) String facultyEmail) {
        try {
            List<SuggestedCourse> list = (facultyEmail != null && !facultyEmail.isEmpty()) 
                ? suggestedCourseRepository.findByFacultyEmail(facultyEmail.trim().toLowerCase())
                : suggestedCourseRepository.findAll();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @Transactional
    @DeleteMapping("/recommend/{id}")
    public ResponseEntity<?> deleteRecommendation(@PathVariable Long id) {
        try {
            suggestedCourseRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Recommendation deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // Cohorts compatibility endpoint for mobile apps
    @GetMapping("/cohorts")
    public ResponseEntity<?> getCohorts(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String branch) {
        try {
            User caller = resolveFacultyCaller(authHeader);
            String b = branch != null ? branch.trim() : "";
            if (caller != null && "FACULTY".equalsIgnoreCase(caller.getRole()) && caller.getBranch() != null && !caller.getBranch().trim().isEmpty()) {
                b = LearningPathService.normalizeBranch(caller.getBranch());
            }

            final String filterBranch = b.isEmpty() ? "" : LearningPathService.normalizeBranch(b);
            List<User> students = userRepository.findAll().stream()
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                    .filter(u -> filterBranch.isEmpty() || (u.getBranch() != null && LearningPathService.normalizeBranch(u.getBranch()).equalsIgnoreCase(filterBranch)))
                    .collect(Collectors.toList());

            Map<String, Long> map = students.stream()
                    .filter(u -> u.getAcademicYear() != null && u.getSemester() != null)
                    .collect(Collectors.groupingBy(u -> u.getAcademicYear() + " - " + u.getSemester(), Collectors.counting()));

            List<Map<String, Object>> cohorts = new ArrayList<>();
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                cohorts.add(Map.of("cohortName", entry.getKey(), "count", entry.getValue()));
            }
            return ResponseEntity.ok(cohorts);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/cohort-stats")
    public ResponseEntity<?> getCohortStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String cohortName,
            @RequestParam(required = false) String branch) {
        return getFacultyOverview(authHeader, branch);
    }
}
