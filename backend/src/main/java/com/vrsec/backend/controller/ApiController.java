package com.vrsec.backend.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vrsec.backend.model.Course;
import com.vrsec.backend.model.CoursePhase;
import com.vrsec.backend.model.QuizAttempt;
import com.vrsec.backend.model.SuggestedCourse;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.CoursePhaseRepository;
import com.vrsec.backend.repository.CourseRepository;
import com.vrsec.backend.repository.QuizAttemptRepository;
import com.vrsec.backend.repository.SuggestedCourseRepository;
import com.vrsec.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/main")
@CrossOrigin(origins = "*")
public class ApiController {

    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private CoursePhaseRepository phaseRepo;
    @Autowired private QuizAttemptRepository quizRepo;
    @Autowired private SuggestedCourseRepository suggestedRepo;

    @PostMapping("/auth")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> req) {
        try {
            String rawEmail = req.get("email");
            if (rawEmail == null || rawEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email is required."));
            }
            final String cleanEmail = rawEmail.trim().toLowerCase();
            String role = req.get("role"); 
            String username = req.get("username");
            
            if ("FACULTY".equalsIgnoreCase(role) && !cleanEmail.endsWith("@vrsiddhartha.ac.in")) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Faculty accounts must use a @vrsiddhartha.ac.in email address."));
            }

            List<User> existingUsers = userRepo.findAll().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(cleanEmail))
                    .collect(Collectors.toList());

            if (existingUsers.isEmpty()) {
                User newUser = new User();
                newUser.setEmail(cleanEmail);
                newUser.setPassword(req.get("password") != null ? req.get("password") : "");
                
                String safeUsername = (username != null && !username.trim().isEmpty()) 
                        ? username.trim() 
                        : cleanEmail.split("@")[0];
                newUser.setUsername(safeUsername);
                newUser.setRole(role != null ? role.toUpperCase() : "STUDENT");
                
                if (newUser.getCgpa() == null) newUser.setCgpa(0.0);
                
                userRepo.save(newUser);
                return ResponseEntity.ok(newUser);
            }
            return ResponseEntity.ok(existingUsers.get(0));
        } catch (Exception e) {
            e.printStackTrace();
            String rootCause = e.getCause() != null && e.getCause().getCause() != null 
                    ? e.getCause().getCause().getMessage() 
                    : e.getMessage();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "DB Error: " + rootCause));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestParam String email) {
        try {
            final String cleanEmail = email != null ? email.trim().toLowerCase() : "";
            List<User> matchingUsers = userRepo.findAll().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(cleanEmail))
                    .collect(Collectors.toList());

            if (matchingUsers.isEmpty()) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", "User not found"));
            }
            
            User user = matchingUsers.get(0);
            List<CoursePhase> allPhases = phaseRepo.findAll();
            List<QuizAttempt> userQuizzes = quizRepo.findAll().stream()
                    .filter(q -> q.getUser() != null && user.getId().equals(q.getUser().getId()))
                    .collect(Collectors.toList());
            
            List<Course> allCourses = courseRepo.findAll().stream()
                    .filter(c -> c.getUserEmail() != null && cleanEmail.equalsIgnoreCase(c.getUserEmail().trim()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> activeCourses = allCourses.stream()
                    .filter(c -> !c.isDeleted())
                    .map(c -> mapCourseWithAnalytics(c, allPhases, userQuizzes))
                    .collect(Collectors.toList());

            List<Map<String, Object>> deletedCourses = allCourses.stream()
                    .filter(Course::isDeleted)
                    .map(c -> mapCourseWithAnalytics(c, allPhases, userQuizzes))
                    .collect(Collectors.toList());
            
            Double storedCgpa = user.getCgpa();
            double currentCgpa = storedCgpa != null ? storedCgpa : 0.0;
            
            List<Map<String, String>> recommendations = new ArrayList<>();

            String studentYear = user.getAcademicYear() != null ? user.getAcademicYear().trim().toLowerCase() : "";
            String studentSem = user.getSemester() != null ? user.getSemester().trim().toLowerCase() : "";
            String studentCohort = studentYear + " year, " + studentSem;

            if (!studentYear.isEmpty() && !studentSem.isEmpty() && cleanEmail.endsWith("@vrsec.ac.in")) {
                List<SuggestedCourse> dynamicRecs = suggestedRepo.findAll().stream()
                    .filter(s -> {
                        String target = s.getTargetSemester() != null ? s.getTargetSemester().trim().toLowerCase() : "";
                        return studentCohort.equals(target);
                    })
                    .collect(Collectors.toList());
                for (SuggestedCourse sc : dynamicRecs) {
                    recommendations.add(Map.of(
                        "title", sc.getTitle(), 
                        "reason", "Mandatory module recommended by your faculty.", 
                        "suggestedBy", sc.getFacultyName() != null ? sc.getFacultyName() : "Faculty"
                    ));
                }
            }

            if ("STUDENT".equals(user.getRole())) {
                String branch = user.getBranch() != null ? user.getBranch().toLowerCase().trim() : "";

                boolean isFirstSem = studentSem.contains("1") || studentSem.contains("first");

                if (branch.contains("cse") || branch.contains("computer") || branch.contains("it")) {
                    if (studentYear.contains("1") || studentYear.contains("first")) {
                        if (isFirstSem) {
                            recommendations.add(Map.of("title", "Foundations of C Programming", "reason", "Core 1st Year Sem 1 requirement for CSE.", "suggestedBy", "System AI"));
                            recommendations.add(Map.of("title", "Engineering Mathematics I", "reason", "Essential calculus framework.", "suggestedBy", "System AI"));
                        } else {
                            recommendations.add(Map.of("title", "Object-Oriented Programming in C++", "reason", "Core 1st Year Sem 2 requirement.", "suggestedBy", "System AI"));
                            recommendations.add(Map.of("title", "Data Structures Basics", "reason", "Introduction to linear data structures.", "suggestedBy", "System AI"));
                        }
                    } else if (studentYear.contains("2") || studentYear.contains("second")) {
                        if (isFirstSem) {
                            recommendations.add(Map.of("title", "Advanced Data Structures & Algorithms", "reason", "Core 2nd Year Sem 1 pillar.", "suggestedBy", "System AI"));
                            recommendations.add(Map.of("title", "Discrete Mathematical Structures", "reason", "Logic and set theory framework.", "suggestedBy", "System AI"));
                        } else {
                            recommendations.add(Map.of("title", "Database Management Systems (DBMS)", "reason", "Core 2nd Year Sem 2 architecture.", "suggestedBy", "System AI"));
                            recommendations.add(Map.of("title", "Operating System Kernels", "reason", "Process management and memory allocation.", "suggestedBy", "System AI"));
                        }
                    } else {
                        if (isFirstSem) {
                            recommendations.add(Map.of("title", "Full-Stack Web Architectures", "reason", "3rd Year Sem 1 enterprise application design.", "suggestedBy", "System AI"));
                        } else {
                            recommendations.add(Map.of("title", "Distributed Cloud Microservices", "reason", "3rd Year Sem 2 scaling standards.", "suggestedBy", "System AI"));
                        }
                    }
                } else if (branch.contains("ece") || branch.contains("electronics")) {
                    if (isFirstSem) {
                        recommendations.add(Map.of("title", "Electronic Circuit Analysis", "reason", "Core ECE semester foundation.", "suggestedBy", "System AI"));
                    } else {
                        recommendations.add(Map.of("title", "Digital Signal Processing", "reason", "Advanced signals coursework.", "suggestedBy", "System AI"));
                    }
                } else if (branch.contains("mech") || branch.contains("mechanical")) {
                    if (isFirstSem) {
                        recommendations.add(Map.of("title", "Advanced Thermodynamics & Heat Transfer", "reason", "Core mechanical engineering pillar.", "suggestedBy", "System AI"));
                    } else {
                        recommendations.add(Map.of("title", "Computer-Aided Machine Design (CAD)", "reason", "Industry standard prototyping tool.", "suggestedBy", "System AI"));
                    }
                } else {
                    if (isFirstSem) {
                        recommendations.add(Map.of("title", "Engineering Core Computing", "reason", "Cross-disciplinary semester foundations.", "suggestedBy", "System AI"));
                    } else {
                        recommendations.add(Map.of("title", "Applied Systems Modeling", "reason", "Advanced cross-disciplinary analysis.", "suggestedBy", "System AI"));
                    }
                }

                if (currentCgpa > 0.0) {
                    if (currentCgpa < 7.0) {
                        recommendations.add(Map.of("title", "Academic Remedial & Concepts Booster", "reason", "Targeted fundamental catch-up path for CGPA under 7.0.", "suggestedBy", "System AI"));
                    } else {
                        recommendations.add(Map.of("title", "Advanced Honors Research & System Optimization", "reason", "High-performance track for students with CGPA 7.0 and above.", "suggestedBy", "System AI"));
                    }
                }
            }

            Map<String, Object> res = new HashMap<>();
            res.put("profile", user);
            res.put("activeCourses", activeCourses);
            res.put("deletedCourses", deletedCourses);
            res.put("cgpa", currentCgpa);
            res.put("recommendations", recommendations);
            
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/generate-path")
    public ResponseEntity<?> generatePath(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            String subject = req.get("subject");
            Course c = new Course();
            c.setTitle(subject);
            c.setProgress(0);
            c.setDifficulty("Standard");
            c.setUserEmail(email);
            c.setDeleted(false);
            Course savedCourse = courseRepo.save(c);
            
            String[] phaseTitles = {"Foundations", "Core Logic", "Advanced Implementation", "Final Assessment"};
            String[][] phaseConcepts = {
                {"Core Architecture : JVM, Bytecode, Memory Allocation", "Environment Setup : IDE Setup, Path Variables", "Fundamental Types : Primitives, Objects, Casting"},
                {"Advanced Control Flow : Loops, Conditionals, Switches", "Algorithmic Execution : Big O, Sorting, Searching", "Error Handling : Try/Catch, Custom Exceptions"},
                {"Object-Oriented Design : Inheritance, Polymorphism, Encapsulation", "Third-Party Libraries : Maven, Gradle, APIs", "Database Connectivity : SQL, JPA, Connections"},
                {"Performance Benchmarking : Profiling, Memory Leaks", "Enterprise Scalability : Load Balancing, Microservices", "Final Capstone : Full Integration Project"}
            };

            for (int i = 0; i < 4; i++) {
                CoursePhase cp = new CoursePhase();
                cp.setCourse(savedCourse);
                cp.setPhaseNumber(i + 1);
                cp.setTitle(phaseTitles[i] + " of " + subject);
                cp.setStudyMaterial(String.join("||", phaseConcepts[i]));
                phaseRepo.save(cp);
            }
            return ResponseEntity.ok(Collections.singletonMap("message", "Professional path generated!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/course/toggle-delete")
    public ResponseEntity<?> toggleCourseDelete(@RequestBody Map<String, Object> req) {
        try {
            Long courseId = Long.valueOf(req.get("courseId").toString());
            boolean isDeleted = (Boolean) req.get("isDeleted");
            Optional<Course> cOpt = courseRepo.findById(courseId);
            if (cOpt.isPresent()) {
                Course c = cOpt.get();
                c.setDeleted(isDeleted);
                courseRepo.save(c);
                return ResponseEntity.ok(Collections.singletonMap("message", "Status updated"));
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    @GetMapping("/quiz/dynamic")
    public ResponseEntity<?> getDynamicQuiz(@RequestParam String topic, @RequestParam String course) {
        List<Map<String, Object>> quiz = new ArrayList<>();
        String[] verbs = {"Define the primary purpose of", "Identify the core characteristic of", "Optimize the execution of"};
        for (int i = 0; i < 10; i++) {
            String qText = String.format("%s %s for %s? [ID:%d]", verbs[i%3], topic.split(" of ")[0], course, i);
            List<String> options = Arrays.asList("Implement standard protocols", "Bypass constraints", "Increase timeout", "Deprecate component");
            Map<String, Object> qMap = new HashMap<>();
            qMap.put("q", qText);
            qMap.put("options", options);
            qMap.put("correct", options.get(0)); 
            quiz.add(qMap);
        }
        return ResponseEntity.ok(quiz);
    }

    @PostMapping("/quiz/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody Map<String, Object> req) {
        try {
            String rawEmail = (String) req.get("email");
            final String cleanEmail = rawEmail != null ? rawEmail.trim().toLowerCase() : "";
            int score = Integer.parseInt(req.get("score").toString());
            int total = Integer.parseInt(req.get("total").toString());
            
            List<User> matchingUsers = userRepo.findAll().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(cleanEmail))
                    .collect(Collectors.toList());

            if (!matchingUsers.isEmpty()) {
                QuizAttempt attempt = new QuizAttempt();
                attempt.setUser(matchingUsers.get(0));
                attempt.setScore(score);
                attempt.setTotalQuestions(total);
                attempt.setTopic(req.get("courseTitle") + " | " + req.get("phaseTitle"));
                quizRepo.save(attempt);
            }
            return ResponseEntity.ok(Collections.singletonMap("message", "Quiz saved"));
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    @PostMapping("/profile/update")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> req) {
        try {
            String rawEmail = (String) req.get("email");
            if (rawEmail == null) return ResponseEntity.badRequest().build();
            final String cleanEmail = rawEmail.trim().toLowerCase();

            List<User> matchingUsers = userRepo.findAll().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(cleanEmail))
                    .collect(Collectors.toList());

            if (!matchingUsers.isEmpty()) {
                User u = matchingUsers.get(0);
                if (req.get("username") != null) u.setUsername(req.get("username").toString().trim());
                if (req.get("branch") != null) u.setBranch(req.get("branch").toString().trim());
                if (req.get("year") != null) u.setAcademicYear(req.get("year").toString().trim());
                if (req.get("sem") != null) u.setSemester(req.get("sem").toString().trim());
                if (req.get("cgpa") != null) {
                    try { u.setCgpa(Double.parseDouble(req.get("cgpa").toString().trim())); } catch (Exception e) { }
                }
                userRepo.save(u);
                return ResponseEntity.ok(Collections.singletonMap("message", "Profile updated"));
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private Map<String, Object> mapCourseWithAnalytics(Course c, List<CoursePhase> allPhases, List<QuizAttempt> allQuizzes) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId());
        map.put("title", c.getTitle());
        
        List<Map<String, Object>> phasesList = allPhases.stream()
            .filter(p -> p.getCourse() != null && p.getCourse().getId().equals(c.getId()))
            .map(p -> {
                Map<String, Object> pMap = new HashMap<>();
                pMap.put("id", p.getId());
                pMap.put("title", p.getTitle());
                pMap.put("studyMaterial", p.getStudyMaterial());
                
                boolean isCompleted = allQuizzes.stream().anyMatch(q -> 
                    q.getTopic() != null && q.getTopic().contains(c.getTitle()) && 
                    q.getTopic().contains(p.getTitle()) && ((double) q.getScore() / q.getTotalQuestions()) >= 0.6
                );
                pMap.put("completed", isCompleted);
                return pMap;
            }).collect(Collectors.toList());
            
        map.put("phases", phasesList);

        double avgScore = allQuizzes.stream()
            .filter(q -> q.getTopic() != null && q.getTopic().contains(c.getTitle()))
            .mapToInt(QuizAttempt::getScore).average().orElse(0.0);
            
        map.put("masteryPercentage", (int) Math.min(Math.round((avgScore / 10.0) * 100.0), 100));
        return map;
    }

    @GetMapping("/faculty/cohorts")
    public ResponseEntity<?> getFacultyCohorts(@RequestParam String branch) {
        try {
            String targetBranch = branch != null ? branch.toLowerCase().trim() : "";
            
            List<User> students = userRepo.findAll().stream()
                .filter(u -> "STUDENT".equals(u.getRole()))
                .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                .filter(u -> u.getBranch() != null && u.getBranch().toLowerCase().contains(targetBranch))
                .collect(Collectors.toList());
            
            Map<String, Long> cohortCounts = students.stream()
                .filter(u -> u.getAcademicYear() != null && u.getSemester() != null)
                .collect(Collectors.groupingBy(
                    u -> u.getAcademicYear().trim().toLowerCase() + " year, " + u.getSemester().trim().toLowerCase(), 
                    Collectors.counting()
                ));
                
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<String, Long> entry : cohortCounts.entrySet()) {
                result.add(Map.of("cohortName", entry.getKey(), "count", entry.getValue()));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    @GetMapping("/faculty/cohort-stats")
    public ResponseEntity<?> getCohortStats(@RequestParam String cohortName, @RequestParam String branch) {
        try {
            String targetBranch = branch != null ? branch.toLowerCase().trim() : "";
            String targetCohort = cohortName != null ? cohortName.trim().toLowerCase() : "";
            
            List<User> students = userRepo.findAll().stream()
                .filter(u -> "STUDENT".equals(u.getRole()))
                .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                .filter(u -> u.getBranch() != null && u.getBranch().toLowerCase().contains(targetBranch))
                .filter(u -> u.getAcademicYear() != null && u.getSemester() != null)
                .filter(u -> {
                    String userCohort = u.getAcademicYear().trim().toLowerCase() + " year, " + u.getSemester().trim().toLowerCase();
                    return userCohort.equals(targetCohort);
                })
                .collect(Collectors.toList());
                
            List<String> emails = students.stream().map(User::getEmail).collect(Collectors.toList());
            List<Course> courses = courseRepo.findAll().stream().filter(c -> emails.contains(c.getUserEmail())).collect(Collectors.toList());
            Map<String, Long> dist = courses.stream().collect(Collectors.groupingBy(Course::getTitle, Collectors.counting()));
            
            List<Map<String, Object>> chart = new ArrayList<>();
            long max = students.isEmpty() ? 1 : students.size();
            for (Map.Entry<String, Long> e : dist.entrySet()) {
                chart.add(Map.of("course", e.getKey(), "studentCount", e.getValue(), "percentage", ((double) e.getValue() / max) * 100));
            }
            return ResponseEntity.ok(Map.of("totalStudents", students.size(), "chartData", chart));
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    @PostMapping("/faculty/recommend")
    public ResponseEntity<?> recommendCourse(@RequestBody Map<String, String> req) {
        try {
            SuggestedCourse sc = new SuggestedCourse();
            sc.setTitle(req.get("title"));
            sc.setTargetSemester(req.get("semester").trim().toLowerCase());
            sc.setFacultyEmail(req.get("facultyEmail"));
            sc.setFacultyName(req.get("facultyName"));
            suggestedRepo.save(sc);
            return ResponseEntity.ok(Collections.singletonMap("message", "Broadcasted."));
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    @GetMapping("/faculty/overall-progress")
    public ResponseEntity<?> getOverallProgress(@RequestParam String branch) {
        try {
            String targetBranch = branch != null ? branch.toLowerCase().trim() : "";
            List<User> branchStudents = userRepo.findAll().stream()
                .filter(u -> "STUDENT".equals(u.getRole()))
                .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                .filter(u -> u.getBranch() != null && u.getBranch().toLowerCase().contains(targetBranch))
                .collect(Collectors.toList());

            List<String> emails = branchStudents.stream().map(User::getEmail).collect(Collectors.toList());
            List<Course> allCourses = courseRepo.findAll().stream().filter(c -> emails.contains(c.getUserEmail())).collect(Collectors.toList());
            List<QuizAttempt> allQuizzes = quizRepo.findAll();
            
            long completedCourses = 0;
            Map<String, Integer> completionByCourse = new HashMap<>();

            for (Course c : allCourses) {
                if (allQuizzes.stream().filter(q -> q.getTopic() != null && q.getTopic().contains(c.getTitle())).mapToInt(QuizAttempt::getScore).average().orElse(0) >= 6.0) {
                    completedCourses++;
                    completionByCourse.put(c.getTitle(), completionByCourse.getOrDefault(c.getTitle(), 0) + 1);
                }
            }
            return ResponseEntity.ok(Map.of("totalActive", allCourses.size(), "totalCompleted", completedCourses, "completionBreakdown", completionByCourse));
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }
}