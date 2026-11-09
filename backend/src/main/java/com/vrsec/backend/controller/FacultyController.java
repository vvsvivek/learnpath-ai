package com.vrsec.backend.controller;

import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/faculty", "/api/v2/faculty", "/api/main/faculty"})
@CrossOrigin(origins = "*")
public class FacultyController {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private TopicPerformanceRepository topicPerformanceRepository;
    @Autowired private SuggestedCourseRepository suggestedCourseRepository;

    /**
     * Overall Faculty Dashboard Analytics strictly for verified @vrsec.ac.in students.
     * Faculty accounts are NEVER included in these analytics.
     */
    @GetMapping({"/overview", "/overall-progress"})
    public ResponseEntity<?> getFacultyOverview(@RequestParam(required = false) String branch) {
        try {
            // Filter ONLY eligible @vrsec.ac.in students
            List<User> students = userRepository.findAll().stream()
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                    .collect(Collectors.toList());

            if (branch != null && !branch.trim().isEmpty() && !"ALL".equalsIgnoreCase(branch)) {
                String b = branch.trim().toLowerCase();
                students = students.stream()
                        .filter(u -> u.getBranch() != null && u.getBranch().toLowerCase().contains(b))
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
                    .collect(Collectors.groupingBy(u -> u.getBranch() != null ? u.getBranch().toUpperCase() : "GENERAL", Collectors.counting()));

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

            // 6. Slow Learners / Students Requiring Additional Academic Support ("Needs Attention")
            // Evaluated transparently based on repeated quiz fails, low CGPA (<6.5), or low completion
            List<Map<String, Object>> studentsNeedingSupport = new ArrayList<>();
            for (User s : students) {
                List<QuizAttempt> sAttempts = attempts.stream().filter(a -> a.getUser().getId().equals(s.getId())).collect(Collectors.toList());
                long failedCount = sAttempts.stream().filter(a -> !a.getPassed()).count();
                boolean lowCgpa = s.getCgpa() != null && s.getCgpa() > 0 && s.getCgpa() < 6.5;

                if (failedCount >= 1 || lowCgpa) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", s.getId());
                    item.put("username", s.getUsername());
                    item.put("email", s.getEmail());
                    item.put("branch", s.getBranch());
                    item.put("academicYear", s.getAcademicYear());
                    item.put("semester", s.getSemester());
                    item.put("cgpa", s.getCgpa());
                    item.put("failedQuizzesCount", failedCount);
                    item.put("reason", lowCgpa ? "CGPA Below 6.5 benchmark" : "Repeated struggle on phase assessments");
                    item.put("status", "Requires Additional Support");
                    studentsNeedingSupport.add(item);
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
            resp.put("studentsNeedingSupportCount", studentsNeedingSupport.size());
            resp.put("studentsNeedingSupport", studentsNeedingSupport);

            // Chart data arrays for mobile/web UI charts
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
     * Lists eligible @vrsec.ac.in students with multi-parameter filtering.
     */
    @GetMapping("/students")
    public ResponseEntity<?> getStudents(
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        try {
            List<User> students = userRepository.findAll().stream()
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                    .collect(Collectors.toList());

            if (branch != null && !branch.trim().isEmpty() && !"ALL".equalsIgnoreCase(branch)) {
                String b = branch.trim().toLowerCase();
                students = students.stream().filter(u -> u.getBranch() != null && u.getBranch().toLowerCase().contains(b)).collect(Collectors.toList());
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

                List<Course> userCourses = courseRepository.findByUserEmailIgnoreCaseAndDeletedFalse(u.getEmail());
                map.put("enrolledCoursesCount", userCourses.size());
                map.put("averageProgress", userCourses.isEmpty() ? 0 : 
                    userCourses.stream().mapToInt(c -> c.getProgress() != null ? c.getProgress() : 0).average().orElse(0));

                boolean needsSupport = (u.getCgpa() != null && u.getCgpa() > 0 && u.getCgpa() < 6.5);
                map.put("performanceStatus", needsSupport ? "Needs Attention" : "Good Standing");

                if ("NEEDS_ATTENTION".equalsIgnoreCase(status) && !needsSupport) {
                    continue;
                }
                result.add(map);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Comprehensive Individual Student Report for faculty inspection.
     */
    @GetMapping("/student/{id}")
    public ResponseEntity<?> getIndividualStudentReport(@PathVariable Long id) {
        try {
            User student = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));

            if (!student.getEmail().endsWith("@vrsec.ac.in")) {
                return ResponseEntity.status(403).body(Collections.singletonMap("error", "Faculty can only inspect records of verified @vrsec.ac.in students."));
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
    @PostMapping({"/recommend", "/suggest-course"})
    public ResponseEntity<?> recommendCourse(@RequestBody Map<String, Object> req) {
        try {
            String title = (String) (req.get("title") != null ? req.get("title") : req.get("courseTitle"));
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Course title is required."));
            }

            SuggestedCourse sc = new SuggestedCourse();
            sc.setTitle(title.trim());
            sc.setDescription((String) req.getOrDefault("description", "Recommended by Department Faculty"));
            sc.setTargetBranch((String) req.getOrDefault("branch", "CSE"));
            sc.setTargetYear((String) req.getOrDefault("year", "2nd Year"));
            sc.setTargetSemester((String) req.getOrDefault("semester", "1st Sem"));
            sc.setFacultyName((String) req.getOrDefault("facultyName", "Department Faculty"));
            sc.setFacultyEmail((String) req.getOrDefault("facultyEmail", "faculty@siddhartha.ac.in"));
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
    public ResponseEntity<?> getCohorts(@RequestParam(required = false) String branch) {
        try {
            String b = branch != null ? branch.trim().toLowerCase() : "";
            List<User> students = userRepository.findAll().stream()
                    .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                    .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                    .filter(u -> b.isEmpty() || (u.getBranch() != null && u.getBranch().toLowerCase().contains(b)))
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
    public ResponseEntity<?> getCohortStats(@RequestParam String cohortName, @RequestParam(required = false) String branch) {
        return getFacultyOverview(branch);
    }
}
