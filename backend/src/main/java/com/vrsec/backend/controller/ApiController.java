package com.vrsec.backend.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.vrsec.backend.service.GeminiChatService;

@RestController
@RequestMapping("/api/main")
@CrossOrigin(origins = "*")
public class ApiController {

    @Autowired private UserRepository userRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private CoursePhaseRepository phaseRepo;
    @Autowired private QuizAttemptRepository quizRepo;
    @Autowired private SuggestedCourseRepository suggestedRepo;
    @Autowired private GeminiChatService geminiChatService;
    @Autowired private ApplicationContext applicationContext;

    private static final Map<Long, List<Map<String, String>>> sessionHistories = new ConcurrentHashMap<>();
    private static final Map<Long, Map<String, Object>> sessionDetails = new ConcurrentHashMap<>();
    private static final Map<String, String> otpStorage = new ConcurrentHashMap<>();

    // --- OTP GENERATION & EMAIL DISPATCH ---

    @PostMapping("/auth/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> req) {
        try {
            String rawEmail = req.get("email");
            if (rawEmail == null || rawEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email is required."));
            }
            final String cleanEmail = rawEmail.trim().toLowerCase();
            String role = req.get("role");

            if ("FACULTY".equalsIgnoreCase(role) && !cleanEmail.endsWith("@siddhartha.ac.in")) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Faculty accounts must use a @siddhartha.ac.in email address."));
            }

            String otp = String.valueOf(100000 + new Random().nextInt(900000));
            otpStorage.put(cleanEmail, otp);

            System.out.println("\n==================================================");
            System.out.println("OTP VERIFICATION CODE FOR " + cleanEmail + " : " + otp);
            System.out.println("==================================================\n");

            boolean emailSent = trySendRealEmail(cleanEmail, otp);

            Map<String, Object> res = new HashMap<>();
            res.put("message", "OTP generated successfully.");
            res.put("emailSent", emailSent);
            if (!emailSent) {
                res.put("devOtp", otp);
            }
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to generate OTP."));
        }
    }

    private boolean trySendRealEmail(String toEmail, String otp) {
        try {
            Class<?> senderClass = Class.forName("org.springframework.mail.javamail.JavaMailSender");
            Object mailSender = applicationContext.getBean(senderClass);
            Class<?> msgClass = Class.forName("org.springframework.mail.SimpleMailMessage");
            Object msg = msgClass.getDeclaredConstructor().newInstance();

            msgClass.getMethod("setTo", String[].class).invoke(msg, (Object) new String[]{toEmail});
            msgClass.getMethod("setSubject", String.class).invoke(msg, "LearnPath - Your OTP Verification Code");
            msgClass.getMethod("setText", String.class).invoke(msg,
                    "Hello,\n\nYour 6-digit OTP verification code for LearnPath is: " + otp +
                    "\n\nPlease enter this code in the mobile app to complete your login.\n\nThank you,\nLearnPath AI Team");

            senderClass.getMethod("send", msgClass).invoke(mailSender, msg);
            return true;
        } catch (Exception e) {
            System.out.println("SMTP Email not configured yet (" + e.getMessage() + "). Showing OTP in console/app.");
            return false;
        }
    }

    // --- OTP VERIFICATION & AUTHENTICATION ---

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
            String submittedOtp = req.get("otp");

            if ("FACULTY".equalsIgnoreCase(role) && !cleanEmail.endsWith("@siddhartha.ac.in")) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Faculty accounts must use a @siddhartha.ac.in email address."));
            }

            String expectedOtp = otpStorage.get(cleanEmail);
            if (expectedOtp == null || submittedOtp == null || !expectedOtp.equals(submittedOtp.trim())) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid or expired OTP code. Please try again."));
            }

            // Clear OTP after successful verification
            otpStorage.remove(cleanEmail);

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

    // --- LIVE CONTEXT-AWARE AI CHAT ENDPOINTS ---

    @GetMapping("/chat/sessions")
    public ResponseEntity<?> getChatSessions(@RequestParam String email) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        List<Map<String, Object>> userSessions = new ArrayList<>();
        for (Map<String, Object> details : sessionDetails.values()) {
            if (cleanEmail.equalsIgnoreCase(String.valueOf(details.get("email")))) {
                userSessions.add(details);
            }
        }
        userSessions.sort((a, b) -> Long.compare((Long) b.get("id"), (Long) a.get("id")));
        return ResponseEntity.ok(userSessions);
    }

    @PostMapping("/chat/new")
    public ResponseEntity<?> startNewChatSession(@RequestParam String email) {
        long sessionId = System.currentTimeMillis();
        Map<String, Object> meta = new HashMap<>();
        meta.put("id", sessionId);
        meta.put("email", email != null ? email.trim().toLowerCase() : "");
        meta.put("title", "New Session");
        meta.put("status", "ACTIVE");

        List<Map<String, String>> initialMessages = new ArrayList<>();
        initialMessages.add(Map.of("role", "ai", "content", "Hello! I am your AI Tutor. Ask me any question about your courses, progress, or B.Tech subjects!"));

        sessionDetails.put(sessionId, meta);
        sessionHistories.put(sessionId, initialMessages);
        return ResponseEntity.ok(meta);
    }

    @GetMapping("/chat/history/{sessionId}")
    public ResponseEntity<?> getChatHistory(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionHistories.getOrDefault(sessionId, new ArrayList<>()));
    }

    @PostMapping("/chat/update-status/{sessionId}")
    public ResponseEntity<?> updateChatStatus(@PathVariable Long sessionId, @RequestParam String status) {
        Map<String, Object> meta = sessionDetails.get(sessionId);
        if (meta != null) {
            meta.put("status", status.toUpperCase());
        }
        return ResponseEntity.ok(Collections.singletonMap("message", "Updated"));
    }

    @PostMapping("/chat/send")
    public ResponseEntity<?> sendChatMessage(@RequestBody Map<String, Object> req) {
        String email = req.get("email") != null ? String.valueOf(req.get("email")).trim().toLowerCase() : "";
        String message = req.get("message") != null ? String.valueOf(req.get("message")).trim() : "";
        Long sessionId = req.get("sessionId") != null ? Long.valueOf(String.valueOf(req.get("sessionId"))) : System.currentTimeMillis();

        if (!sessionDetails.containsKey(sessionId)) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("id", sessionId);
            meta.put("email", email);
            meta.put("title", "New Session");
            meta.put("status", "ACTIVE");
            sessionDetails.put(sessionId, meta);
            sessionHistories.put(sessionId, new ArrayList<>());
        }

        String aiReply = geminiChatService.getGenerativeAIResponse(email, message);

        List<Map<String, String>> history = sessionHistories.computeIfAbsent(sessionId, k -> new ArrayList<>());
        history.add(Map.of("role", "user", "content", message));
        history.add(Map.of("role", "ai", "content", aiReply));

        Map<String, Object> meta = sessionDetails.get(sessionId);
        String currentTitle = (String) meta.get("title");
        if (currentTitle == null || "New Session".equals(currentTitle)) {
            String[] words = message.split("\\s+");
            String autoTitle = words.length > 4
                    ? words[0] + " " + words[1] + " " + words[2] + " " + words[3] + "..."
                    : message;
            meta.put("title", autoTitle);
            currentTitle = autoTitle;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("sessionId", sessionId);
        res.put("reply", aiReply);
        res.put("title", currentTitle);
        return ResponseEntity.ok(res);
    }

    // --- DASHBOARD & COURSE ENDPOINTS ---

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

            if (!studentYear.isEmpty() && !studentSem.isEmpty() && (cleanEmail.endsWith("@vrsec.ac.in") || cleanEmail.endsWith("@siddhartha.ac.in"))) {
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

            List<User> matchingUsers = userRepo.findAll().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().trim().equalsIgnoreCase(email.trim()))
                    .collect(Collectors.toList());

            if (matchingUsers.isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "User not found"));
            }

            List<Map<String, Object>> phasesData = geminiChatService.generateCourseCurriculum(subject);

            Course c = new Course();
            c.setTitle(subject.trim());
            c.setProgress(0);
            c.setDifficulty("Standard");
            c.setUserEmail(email.trim().toLowerCase());
            c.setDeleted(false);
            Course savedCourse = courseRepo.save(c);

            int phaseNum = 1;
            for (Map<String, Object> pData : phasesData) {
                CoursePhase cp = new CoursePhase();
                cp.setCourse(savedCourse);
                cp.setPhaseNumber(phaseNum++);
                cp.setTitle(String.valueOf(pData.get("title")));
                cp.setStudyMaterial(String.valueOf(pData.get("studyMaterial")));
                phaseRepo.save(cp);
            }

            return ResponseEntity.ok(Collections.singletonMap("message", "Professional path generated for " + subject + "!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to generate dynamic path."));
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
        try {
            String studyMaterial = phaseRepo.findAll().stream()
                    .filter(p -> p.getCourse() != null &&
                            p.getCourse().getTitle() != null &&
                            p.getCourse().getTitle().trim().equalsIgnoreCase(course.trim()) &&
                            p.getTitle() != null &&
                            p.getTitle().trim().equalsIgnoreCase(topic.trim()))
                    .map(CoursePhase::getStudyMaterial)
                    .findFirst()
                    .orElse("");

            List<Map<String, Object>> quizData = geminiChatService.generatePhaseQuiz(course, topic, studyMaterial);
            return ResponseEntity.ok(quizData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "Failed to generate dynamic quiz."));
        }
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
                return ResponseEntity.ok(u);
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
                .filter(u -> u.getEmail() != null && (u.getEmail().toLowerCase().endsWith("@vrsec.ac.in") || u.getEmail().toLowerCase().endsWith("@siddhartha.ac.in")))
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
                .filter(u -> u.getEmail() != null && (u.getEmail().toLowerCase().endsWith("@vrsec.ac.in") || u.getEmail().toLowerCase().endsWith("@siddhartha.ac.in")))
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
                .filter(u -> u.getEmail() != null && (u.getEmail().toLowerCase().endsWith("@vrsec.ac.in") || u.getEmail().toLowerCase().endsWith("@siddhartha.ac.in")))
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