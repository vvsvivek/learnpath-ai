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
import com.vrsec.backend.service.ResendMailService;

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
    @Autowired private ResendMailService resendMailService;
    @Autowired private ApplicationContext applicationContext;

    private static final Map sessionHistories = new ConcurrentHashMap();
    private static final Map sessionDetails = new ConcurrentHashMap();
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

            boolean emailSent = true;
            try {
                resendMailService.sendOtpEmail(cleanEmail, otp);
                System.out.println("OTP email dispatched via Google Apps Script to: " + cleanEmail);
            } catch (Exception e) {
                System.err.println("Google Apps Script email error: " + e.getMessage());
                emailSent = false;
            }

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
        List userSessions = new ArrayList();
        for (Object val : sessionDetails.values()) {
            Map details = (Map) val;
            if (cleanEmail.equalsIgnoreCase(String.valueOf(details.get("email")))) {
                userSessions.add(details);
            }
        }
        return ResponseEntity.ok(userSessions);
    }

    @PostMapping("/chat/new")
    public ResponseEntity<?> startNewChatSession(@RequestParam String email) {
        long sessionId = System.currentTimeMillis();
        Map meta = new HashMap();
        meta.put("id", sessionId);
        meta.put("email", email != null ? email.trim().toLowerCase() : "");
        meta.put("title", "New Session");
        meta.put("status", "ACTIVE");

        Map initialMsg = new HashMap();
        initialMsg.put("role", "ai");
        initialMsg.put("content", "Hello! I am your AI Tutor. Ask me any question about your courses, progress, or B.Tech subjects!");

        List initialMessages = new ArrayList();
        initialMessages.add(initialMsg);

        sessionDetails.put(sessionId, meta);
        sessionHistories.put(sessionId, initialMessages);
        return ResponseEntity.ok(meta);
    }

    @GetMapping("/chat/history/{sessionId}")
    public ResponseEntity<?> getChatHistory(@PathVariable Long sessionId) {
        Object history = sessionHistories.get(sessionId);
        return ResponseEntity.ok(history != null ? history : new ArrayList());
    }

    @PostMapping("/chat/update-status/{sessionId}")
    public ResponseEntity<?> updateChatStatus(@PathVariable Long sessionId, @RequestParam String status) {
        Map meta = (Map) sessionDetails.get(sessionId);
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
            Map meta = new HashMap();
            meta.put("id", sessionId);
            meta.put("email", email);
            meta.put("title", "New Session");
            meta.put("status", "ACTIVE");
            sessionDetails.put(sessionId, meta);
            sessionHistories.put(sessionId, new ArrayList());
        }

        String aiReply = geminiChatService.getGenerativeAIResponse(email, message);

        Map userMsg = new HashMap();
        userMsg.put("role", "user");
        userMsg.put("content", message);

        Map aiMsg = new HashMap();
        aiMsg.put("role", "ai");
        aiMsg.put("content", aiReply);

        List history = (List) sessionHistories.computeIfAbsent(sessionId, k -> new ArrayList());
        history.add(userMsg);
        history.add(aiMsg);

        Map meta = (Map) sessionDetails.get(sessionId);
        String currentTitle = (String) meta.get("title");
        if (currentTitle == null || "New Session".equals(currentTitle)) {
            String[] words = message.split("\\s+");
            String autoTitle = words.length > 4
                    ? words[0] + " " + words[1] + " " + words[2] + " " + words[3] + "..."
                    : message;
            meta.put("title", autoTitle);
            currentTitle = autoTitle;
        }

        Map res = new HashMap();
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

            List activeCourses = new ArrayList();
            for (Course c : allCourses) {
                if (!c.isDeleted()) {
                    activeCourses.add(mapCourseWithAnalytics(c, allPhases, userQuizzes));
                }
            }

            List deletedCourses = new ArrayList();
            for (Course c : allCourses) {
                if (c.isDeleted()) {
                    deletedCourses.add(mapCourseWithAnalytics(c, allPhases, userQuizzes));
                }
            }

            Double storedCgpa = user.getCgpa();
            double currentCgpa = storedCgpa != null ? storedCgpa : 0.0;

            List recommendations = new ArrayList();

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
                    Map rec = new HashMap();
                    rec.put("title", sc.getTitle());
                    rec.put("reason", "Mandatory module recommended by your faculty.");
                    rec.put("suggestedBy", sc.getFacultyName() != null ? sc.getFacultyName() : "Faculty");
                    recommendations.add(rec);
                }
            }

            if ("STUDENT".equals(user.getRole())) {
                String branch = user.getBranch() != null ? user.getBranch().toLowerCase().trim() : "";
                boolean isFirstSem = studentSem.contains("1") || studentSem.contains("first");

                if (branch.contains("cse") || branch.contains("computer") || branch.contains("it")) {
                    if (studentYear.contains("1") || studentYear.contains("first")) {
                        if (isFirstSem) {
                            recommendations.add(createRec("Foundations of C Programming", "Core 1st Year Sem 1 requirement for CSE.", "System AI"));
                            recommendations.add(createRec("Engineering Mathematics I", "Essential calculus framework.", "System AI"));
                        } else {
                            recommendations.add(createRec("Object-Oriented Programming in C++", "Core 1st Year Sem 2 requirement.", "System AI"));
                            recommendations.add(createRec("Data Structures Basics", "Introduction to linear data structures.", "System AI"));
                        }
                    } else if (studentYear.contains("2") || studentYear.contains("second")) {
                        if (isFirstSem) {
                            recommendations.add(createRec("Advanced Data Structures & Algorithms", "Core 2nd Year Sem 1 pillar.", "System AI"));
                            recommendations.add(createRec("Discrete Mathematical Structures", "Logic and set theory framework.", "System AI"));
                        } else {
                            recommendations.add(createRec("Database Management Systems (DBMS)", "Core 2nd Year Sem 2 architecture.", "System AI"));
                            recommendations.add(createRec("Operating System Kernels", "Process management and memory allocation.", "System AI"));
                        }
                    } else {
                        if (isFirstSem) {
                            recommendations.add(createRec("Full-Stack Web Architectures", "3rd Year Sem 1 enterprise application design.", "System AI"));
                        } else {
                            recommendations.add(createRec("Distributed Cloud Microservices", "3rd Year Sem 2 scaling standards.", "System AI"));
                        }
                    }
                } else if (branch.contains("ece") || branch.contains("electronics")) {
                    if (isFirstSem) {
                        recommendations.add(createRec("Electronic Circuit Analysis", "Core ECE semester foundation.", "System AI"));
                    } else {
                        recommendations.add(createRec("Digital Signal Processing", "Advanced signals coursework.", "System AI"));
                    }
                } else if (branch.contains("mech") || branch.contains("mechanical")) {
                    if (isFirstSem) {
                        recommendations.add(createRec("Advanced Thermodynamics & Heat Transfer", "Core mechanical engineering pillar.", "System AI"));
                    } else {
                        recommendations.add(createRec("Computer-Aided Machine Design (CAD)", "Industry standard prototyping tool.", "System AI"));
                    }
                } else {
                    if (isFirstSem) {
                        recommendations.add(createRec("Engineering Core Computing", "Cross-disciplinary semester foundations.", "System AI"));
                    } else {
                        recommendations.add(createRec("Applied Systems Modeling", "Advanced cross-disciplinary analysis.", "System AI"));
                    }
                }

                if (currentCgpa > 0.0) {
                    if (currentCgpa < 7.0) {
                        recommendations.add(createRec("Academic Remedial & Concepts Booster", "Targeted fundamental catch-up path for CGPA under 7.0.", "System AI"));
                    } else {
                        recommendations.add(createRec("Advanced Honors Research & System Optimization", "High-performance track for students with CGPA 7.0 and above.", "System AI"));
                    }
                }
            }

            Map res = new HashMap();
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

    private Map createRec(String title, String reason, String suggestedBy) {
        Map m = new HashMap();
        m.put("title", title);
        m.put("reason", reason);
        m.put("suggestedBy", suggestedBy);
        return m;
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

            List phasesData = (List) geminiChatService.generateCourseCurriculum(subject);

            Course c = new Course();
            c.setTitle(subject.trim());
            c.setProgress(0);
            c.setDifficulty("Standard");
            c.setUserEmail(email.trim().toLowerCase());
            c.setDeleted(false);
            Course savedCourse = courseRepo.save(c);

            int phaseNum = 1;
            for (Object obj : phasesData) {
                Map pData = (Map) obj;
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

            List quizData = geminiChatService.generatePhaseQuiz(course, topic, studyMaterial);
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

    private Map mapCourseWithAnalytics(Course c, List<CoursePhase> allPhases, List<QuizAttempt> allQuizzes) {
        Map map = new HashMap();
        map.put("id", c.getId());
        map.put("title", c.getTitle());

        List phasesList = new ArrayList();
        for (CoursePhase p : allPhases) {
            if (p.getCourse() != null && p.getCourse().getId().equals(c.getId())) {
                Map pMap = new HashMap();
                pMap.put("id", p.getId());
                pMap.put("title", p.getTitle());
                pMap.put("studyMaterial", p.getStudyMaterial());

                boolean isCompleted = allQuizzes.stream().anyMatch(q ->
                    q.getTopic() != null && q.getTopic().contains(c.getTitle()) &&
                    q.getTopic().contains(p.getTitle()) && ((double) q.getScore() / q.getTotalQuestions()) >= 0.6
                );
                pMap.put("completed", isCompleted);
                phasesList.add(pMap);
            }
        }

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

            Map cohortCounts = new HashMap();
            for (User u : students) {
                if (u.getAcademicYear() != null && u.getSemester() != null) {
                    String key = u.getAcademicYear().trim().toLowerCase() + " year, " + u.getSemester().trim().toLowerCase();
                    cohortCounts.put(key, (Long) cohortCounts.getOrDefault(key, 0L) + 1L);
                }
            }

            List result = new ArrayList();
            for (Object keyObj : cohortCounts.keySet()) {
                String key = (String) keyObj;
                Map row = new HashMap();
                row.put("cohortName", key);
                row.put("count", cohortCounts.get(key));
                result.add(row);
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
            
            Map dist = new HashMap();
            for (Course c : courses) {
                String title = c.getTitle();
                dist.put(title, (Long) dist.getOrDefault(title, 0L) + 1L);
            }

            List chart = new ArrayList();
            long max = students.isEmpty() ? 1 : students.size();
            for (Object keyObj : dist.keySet()) {
                String title = (String) keyObj;
                Long count = (Long) dist.get(title);
                Map chartRow = new HashMap();
                chartRow.put("course", title);
                chartRow.put("studentCount", count);
                chartRow.put("percentage", ((double) count / max) * 100);
                chart.add(chartRow);
            }
            
            Map resp = new HashMap();
            resp.put("totalStudents", students.size());
            resp.put("chartData", chart);
            return ResponseEntity.ok(resp);
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
            Map completionByCourse = new HashMap();

            for (Course c : allCourses) {
                boolean passed = allQuizzes.stream().filter(q -> q.getTopic() != null && q.getTopic().contains(c.getTitle())).mapToInt(QuizAttempt::getScore).average().orElse(0) >= 6.0;
                if (passed) {
                    completedCourses++;
                    String title = c.getTitle();
                    completionByCourse.put(title, (Integer) completionByCourse.getOrDefault(title, 0) + 1);
                }
            }
            
            Map resp = new HashMap();
            resp.put("totalActive", allCourses.size());
            resp.put("totalCompleted", completedCourses);
            resp.put("completionBreakdown", completionByCourse);
            return ResponseEntity.ok(resp);
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }
}