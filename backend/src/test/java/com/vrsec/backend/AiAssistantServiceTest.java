package com.vrsec.backend;

import com.vrsec.backend.controller.ChatController;
import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import com.vrsec.backend.service.AiAssistantService;
import com.vrsec.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class AiAssistantServiceTest {

    private ChatController chatController;
    private AiAssistantService aiAssistantService;
    private JwtService jwtService;

    private Map<String, User> userDb;
    private Map<Long, Course> courseDb;
    private Map<Long, CoursePhase> phaseDb;
    private List<QuizAttempt> quizAttemptDb;
    private List<TopicPerformance> topicPerformanceDb;
    private List<SuggestedCourse> suggestedCourseDb;
    private Map<Long, ChatSession> sessionDb;
    private List<ChatMessage> messageDb;

    private String studentToken;
    private String student2Token;
    private String facultyToken;
    private String hodToken;
    private String adminToken;
    private String mechFacultyToken;

    @BeforeEach
    void setUp() {
        chatController = new ChatController();
        aiAssistantService = new AiAssistantService();
        jwtService = new JwtService();
        jwtService.init();

        userDb = new ConcurrentHashMap<>();
        courseDb = new ConcurrentHashMap<>();
        phaseDb = new ConcurrentHashMap<>();
        quizAttemptDb = new ArrayList<>();
        topicPerformanceDb = new ArrayList<>();
        suggestedCourseDb = new ArrayList<>();
        sessionDb = new ConcurrentHashMap<>();
        messageDb = new ArrayList<>();

        // 1. Mock UserRepository
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByEmail".equals(name)) {
                        String email = (String) args[0];
                        return Optional.ofNullable(userDb.get(email != null ? email.toLowerCase() : null));
                    }
                    if ("findAll".equals(name)) {
                        return new ArrayList<>(userDb.values());
                    }
                    if ("save".equals(name)) {
                        User u = (User) args[0];
                        if (u.getId() == null) u.setId((long) (userDb.size() + 1));
                        userDb.put(u.getEmail().toLowerCase(), u);
                        return u;
                    }
                    return null;
                });

        // 2. Mock CourseRepository
        CourseRepository courseRepository = (CourseRepository) Proxy.newProxyInstance(
                CourseRepository.class.getClassLoader(),
                new Class<?>[]{CourseRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByUserEmailIgnoreCaseAndDeletedFalse".equals(name)) {
                        String email = (String) args[0];
                        return courseDb.values().stream()
                                .filter(c -> c.getUserEmail() != null && c.getUserEmail().equalsIgnoreCase(email) && !c.isDeleted())
                                .collect(Collectors.toList());
                    }
                    if ("findByUserEmailIn".equals(name)) {
                        Collection<String> emails = (Collection<String>) args[0];
                        return courseDb.values().stream()
                                .filter(c -> emails.contains(c.getUserEmail()))
                                .collect(Collectors.toList());
                    }
                    if ("findAll".equals(name)) {
                        return new ArrayList<>(courseDb.values());
                    }
                    if ("save".equals(name)) {
                        Course c = (Course) args[0];
                        if (c.getId() == null) c.setId((long) (courseDb.size() + 1));
                        courseDb.put(c.getId(), c);
                        return c;
                    }
                    return null;
                });

        // 3. Mock CoursePhaseRepository
        CoursePhaseRepository phaseRepository = (CoursePhaseRepository) Proxy.newProxyInstance(
                CoursePhaseRepository.class.getClassLoader(),
                new Class<?>[]{CoursePhaseRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByCourseIdOrderByPhaseNumberAsc".equals(name)) {
                        Long courseId = (Long) args[0];
                        return phaseDb.values().stream()
                                .filter(p -> p.getCourse() != null && courseId.equals(p.getCourse().getId()))
                                .sorted(Comparator.comparingInt(CoursePhase::getPhaseNumber))
                                .collect(Collectors.toList());
                    }
                    if ("save".equals(name)) {
                        CoursePhase cp = (CoursePhase) args[0];
                        if (cp.getId() == null) cp.setId((long) (phaseDb.size() + 1));
                        phaseDb.put(cp.getId(), cp);
                        return cp;
                    }
                    return null;
                });

        // 4. Mock QuizAttemptRepository
        QuizAttemptRepository quizAttemptRepository = (QuizAttemptRepository) Proxy.newProxyInstance(
                QuizAttemptRepository.class.getClassLoader(),
                new Class<?>[]{QuizAttemptRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByUserIdOrderByCreatedAtDesc".equals(name)) {
                        Long uid = (Long) args[0];
                        return quizAttemptDb.stream()
                                .filter(q -> q.getUser() != null && uid.equals(q.getUser().getId()))
                                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .collect(Collectors.toList());
                    }
                    if ("findAll".equals(name)) {
                        return new ArrayList<>(quizAttemptDb);
                    }
                    if ("save".equals(name)) {
                        QuizAttempt q = (QuizAttempt) args[0];
                        quizAttemptDb.add(q);
                        return q;
                    }
                    return null;
                });

        // 5. Mock TopicPerformanceRepository
        TopicPerformanceRepository topicPerformanceRepository = (TopicPerformanceRepository) Proxy.newProxyInstance(
                TopicPerformanceRepository.class.getClassLoader(),
                new Class<?>[]{TopicPerformanceRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByUserId".equals(name)) {
                        Long uid = (Long) args[0];
                        return topicPerformanceDb.stream()
                                .filter(t -> t.getUser() != null && uid.equals(t.getUser().getId()))
                                .collect(Collectors.toList());
                    }
                    return Collections.emptyList();
                });

        // 6. Mock SuggestedCourseRepository
        SuggestedCourseRepository suggestedCourseRepository = (SuggestedCourseRepository) Proxy.newProxyInstance(
                SuggestedCourseRepository.class.getClassLoader(),
                new Class<?>[]{SuggestedCourseRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByFacultyEmail".equals(name)) {
                        String fe = (String) args[0];
                        return suggestedCourseDb.stream()
                                .filter(s -> s.getFacultyEmail() != null && s.getFacultyEmail().equalsIgnoreCase(fe))
                                .collect(Collectors.toList());
                    }
                    return Collections.emptyList();
                });

        // 7. Mock ChatSessionRepository
        ChatSessionRepository chatSessionRepository = (ChatSessionRepository) Proxy.newProxyInstance(
                ChatSessionRepository.class.getClassLoader(),
                new Class<?>[]{ChatSessionRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByUserEmailIgnoreCaseOrderByUpdatedAtDesc".equals(name)) {
                        String email = (String) args[0];
                        return sessionDb.values().stream()
                                .filter(s -> s.getUserEmail() != null && s.getUserEmail().equalsIgnoreCase(email))
                                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                                .collect(Collectors.toList());
                    }
                    if ("findById".equals(name)) {
                        Long sid = (Long) args[0];
                        return Optional.ofNullable(sessionDb.get(sid));
                    }
                    if ("save".equals(name)) {
                        ChatSession s = (ChatSession) args[0];
                        if (s.getId() == null) s.setId((long) (sessionDb.size() + 1));
                        if (s.getUpdatedAt() == null) s.setUpdatedAt(LocalDateTime.now());
                        sessionDb.put(s.getId(), s);
                        return s;
                    }
                    return null;
                });

        // 8. Mock ChatMessageRepository
        ChatMessageRepository chatMessageRepository = (ChatMessageRepository) Proxy.newProxyInstance(
                ChatMessageRepository.class.getClassLoader(),
                new Class<?>[]{ChatMessageRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findBySessionIdOrderByTimestampAsc".equals(name)) {
                        Long sid = (Long) args[0];
                        return messageDb.stream()
                                .filter(m -> m.getSession() != null && sid.equals(m.getSession().getId()))
                                .sorted(Comparator.comparing(ChatMessage::getTimestamp))
                                .collect(Collectors.toList());
                    }
                    if ("save".equals(name)) {
                        ChatMessage m = (ChatMessage) args[0];
                        if (m.getId() == null) m.setId((long) (messageDb.size() + 1));
                        messageDb.add(m);
                        return m;
                    }
                    return null;
                });

        // Wire dependencies into AiAssistantService
        ReflectionTestUtils.setField(aiAssistantService, "userRepository", userRepository);
        ReflectionTestUtils.setField(aiAssistantService, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(aiAssistantService, "phaseRepository", phaseRepository);
        ReflectionTestUtils.setField(aiAssistantService, "quizAttemptRepository", quizAttemptRepository);
        ReflectionTestUtils.setField(aiAssistantService, "topicPerformanceRepository", topicPerformanceRepository);
        ReflectionTestUtils.setField(aiAssistantService, "suggestedCourseRepository", suggestedCourseRepository);
        ReflectionTestUtils.setField(aiAssistantService, "chatSessionRepository", chatSessionRepository);
        ReflectionTestUtils.setField(aiAssistantService, "chatMessageRepository", chatMessageRepository);

        // Wire dependencies into ChatController
        ReflectionTestUtils.setField(chatController, "aiAssistantService", aiAssistantService);
        ReflectionTestUtils.setField(chatController, "chatSessionRepository", chatSessionRepository);
        ReflectionTestUtils.setField(chatController, "chatMessageRepository", chatMessageRepository);
        ReflectionTestUtils.setField(chatController, "userRepository", userRepository);
        ReflectionTestUtils.setField(chatController, "jwtService", jwtService);

        // Seed test accounts
        User student1 = new User();
        student1.setId(1L);
        student1.setEmail("student1@vrsec.ac.in");
        student1.setUsername("Aarav Sharma");
        student1.setRole("STUDENT");
        student1.setBranch("CSE");
        student1.setAcademicYear("3rd Year");
        student1.setSemester("1st Sem");
        student1.setCgpa(8.4);
        student1.setIsEmailVerified(true);
        userDb.put(student1.getEmail().toLowerCase(), student1);

        User student2 = new User();
        student2.setId(2L);
        student2.setEmail("student2@vrsec.ac.in");
        student2.setUsername("Bhavya Patel");
        student2.setRole("STUDENT");
        student2.setBranch("CSE");
        student2.setAcademicYear("2nd Year");
        student2.setSemester("2nd Sem");
        student2.setCgpa(6.2); // Needs support
        student2.setIsEmailVerified(true);
        userDb.put(student2.getEmail().toLowerCase(), student2);

        // Student with external domain (Gmail) in CSE department
        User studentExternal = new User();
        studentExternal.setId(5L);
        studentExternal.setEmail("student.external@gmail.com");
        studentExternal.setUsername("Kiran Varma");
        studentExternal.setRole("STUDENT");
        studentExternal.setBranch("CSE");
        studentExternal.setAcademicYear("1st Year");
        studentExternal.setSemester("1st Sem");
        studentExternal.setCgpa(7.5);
        studentExternal.setIsEmailVerified(true);
        userDb.put(studentExternal.getEmail().toLowerCase(), studentExternal);

        // Student in MECH department
        User studentMech = new User();
        studentMech.setId(6L);
        studentMech.setEmail("mech.student@outlook.com");
        studentMech.setUsername("Suresh Rao");
        studentMech.setRole("STUDENT");
        studentMech.setBranch("MECH");
        studentMech.setCgpa(7.1);
        studentMech.setIsEmailVerified(true);
        userDb.put(studentMech.getEmail().toLowerCase(), studentMech);

        User facultyCSE = new User();
        facultyCSE.setId(3L);
        facultyCSE.setEmail("faculty.cse@siddhartha.ac.in");
        facultyCSE.setUsername("Prof. M. Prasad");
        facultyCSE.setRole("FACULTY");
        facultyCSE.setBranch("CSE");
        facultyCSE.setIsEmailVerified(true);
        userDb.put(facultyCSE.getEmail().toLowerCase(), facultyCSE);

        // Departmental HOD (Designated course approver for CSE)
        User hodCSE = new User();
        hodCSE.setId(7L);
        hodCSE.setEmail("hod.cse@siddhartha.ac.in");
        hodCSE.setUsername("Dr. K. Srinivas");
        hodCSE.setRole("FACULTY");
        hodCSE.setBranch("CSE");
        hodCSE.setIsEmailVerified(true);
        userDb.put(hodCSE.getEmail().toLowerCase(), hodCSE);

        // Institutional Administrator
        User admin = new User();
        admin.setId(8L);
        admin.setEmail("admin@siddhartha.ac.in");
        admin.setUsername("System Admin");
        admin.setRole("ADMIN");
        admin.setIsEmailVerified(true);
        userDb.put(admin.getEmail().toLowerCase(), admin);

        User facultyMech = new User();
        facultyMech.setId(4L);
        facultyMech.setEmail("faculty.mech@siddhartha.ac.in");
        facultyMech.setUsername("Prof. R. Kumar");
        facultyMech.setRole("FACULTY");
        facultyMech.setBranch("MECH");
        facultyMech.setIsEmailVerified(true);
        userDb.put(facultyMech.getEmail().toLowerCase(), facultyMech);

        // Seed courses for student1
        Course c1 = new Course();
        c1.setId(101L);
        c1.setTitle("Database Management Systems");
        c1.setProgress(50);
        c1.setCurrentPhase(2);
        c1.setTotalPhases(4);
        c1.setUserEmail(student1.getEmail());
        courseDb.put(c1.getId(), c1);

        CoursePhase cp1 = new CoursePhase();
        cp1.setId(201L);
        cp1.setCourse(c1);
        cp1.setPhaseNumber(1);
        cp1.setTitle("ER Modeling & Relational Algebra");
        cp1.setCompleted(true);
        phaseDb.put(cp1.getId(), cp1);

        CoursePhase cp2 = new CoursePhase();
        cp2.setId(202L);
        cp2.setCourse(c1);
        cp2.setPhaseNumber(2);
        cp2.setTitle("Normalization: 1NF, 2NF, 3NF & BCNF");
        cp2.setCompleted(false);
        cp2.setUnlocked(true);
        phaseDb.put(cp2.getId(), cp2);

        // Seed quiz attempt for student1
        QuizAttempt qa1 = new QuizAttempt();
        qa1.setId(301L);
        qa1.setUser(student1);
        qa1.setCourse(c1);
        qa1.setTopic("Normalization");
        qa1.setScore(8);
        qa1.setTotalQuestions(10);
        qa1.setPassed(true);
        quizAttemptDb.add(qa1);

        // Seed weak topic
        TopicPerformance tp1 = new TopicPerformance();
        tp1.setId(401L);
        tp1.setUser(student1);
        tp1.setCourseTitle("Database Management Systems");
        tp1.setConceptTag("Transitive Dependencies in 3NF");
        tp1.setTimesTested(5);
        tp1.setTimesIncorrect(3);
        ReflectionTestUtils.setField(tp1, "status", "NEEDS_ATTENTION");
        topicPerformanceDb.add(tp1);

        // Generate valid JWT tokens
        studentToken = "Bearer " + jwtService.generateToken(student1.getEmail(), "STUDENT", student1.getId());
        student2Token = "Bearer " + jwtService.generateToken(student2.getEmail(), "STUDENT", student2.getId());
        facultyToken = "Bearer " + jwtService.generateToken(facultyCSE.getEmail(), "FACULTY", facultyCSE.getId());
        hodToken = "Bearer " + jwtService.generateToken(hodCSE.getEmail(), "FACULTY", hodCSE.getId());
        adminToken = "Bearer " + jwtService.generateToken(admin.getEmail(), "ADMIN", admin.getId());
        mechFacultyToken = "Bearer " + jwtService.generateToken(facultyMech.getEmail(), "FACULTY", facultyMech.getId());
    }

    // =========================================================================
    // 1. AUTHENTICATION & SECURITY TESTS
    // =========================================================================
    @Test
    void testChatEndpoints_RequireBearerJwt_RejectMissingToken() {
        ResponseEntity<?> resp = chatController.sendMessage(null, Map.of("message", "What is normalization?"));
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertTrue(resp.getBody().toString().contains("Authentication required"));
    }

    @Test
    void testChatEndpoints_RejectInvalidOrMalformedToken() {
        ResponseEntity<?> resp = chatController.sendMessage("Bearer invalid.jwt.token", Map.of("message", "Hello"));
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void testChatEndpoints_IgnoreEmailInBody_DeriveIdentityFromJwt() {
        Map<String, Object> req = new HashMap<>();
        req.put("email", "student2@vrsec.ac.in"); // Spoofed
        req.put("message", "What courses am I enrolled in?");

        ResponseEntity<?> resp = chatController.sendMessage(studentToken, req);
        assertNotEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertNotEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // =========================================================================
    // 2. SESSION OWNERSHIP & CROSS-USER ISOLATION TESTS
    // =========================================================================
    @Test
    void testSessionOwnership_StudentCannotSendMessageToAnotherStudentsSession() {
        ChatSession s1 = new ChatSession();
        s1.setUserEmail("student1@vrsec.ac.in");
        s1.setTitle("Aarav Study Session");
        s1.setUser(userDb.get("student1@vrsec.ac.in"));
        s1.setId(10L);
        sessionDb.put(10L, s1);

        Map<String, Object> req = Map.of("sessionId", 10, "message", "Hacking session");
        ResponseEntity<?> resp = chatController.sendMessage(student2Token, req);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertTrue(resp.getBody().toString().contains("Access denied"));
    }

    @Test
    void testSessionHistory_StudentCannotReadAnotherStudentsHistory() {
        ChatSession s1 = new ChatSession();
        s1.setUserEmail("student1@vrsec.ac.in");
        s1.setTitle("Private Session");
        s1.setId(20L);
        sessionDb.put(20L, s1);

        messageDb.add(new ChatMessage(s1, "user", "My secret query"));
        messageDb.add(new ChatMessage(s1, "ai", "Your secret answer"));

        ResponseEntity<?> resp = chatController.getHistory(student2Token, 20L);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void testSessionHistory_OwnerCanReadTheirOwnHistory() {
        ChatSession s1 = new ChatSession();
        s1.setUserEmail("student1@vrsec.ac.in");
        s1.setTitle("My Own Session");
        s1.setId(30L);
        sessionDb.put(30L, s1);

        messageDb.add(new ChatMessage(s1, "user", "Explain 2NF"));
        messageDb.add(new ChatMessage(s1, "ai", "2NF eliminates partial dependencies"));

        ResponseEntity<?> resp = chatController.getHistory(studentToken, 30L);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<?> history = (List<?>) resp.getBody();
        assertEquals(2, history.size());
    }

    @Test
    void testSessionDeletion_OnlyOwnerCanDeleteSession() {
        ChatSession s1 = new ChatSession();
        s1.setUserEmail("student1@vrsec.ac.in");
        s1.setTitle("Session to Delete");
        s1.setId(40L);
        sessionDb.put(40L, s1);

        // Student 2 attempts to delete Student 1's session -> 403
        ResponseEntity<?> reject = chatController.deleteSession(student2Token, 40L);
        assertEquals(HttpStatus.FORBIDDEN, reject.getStatusCode());

        // Student 1 deletes their own session -> 200
        ResponseEntity<?> ok = chatController.deleteSession(studentToken, 40L);
        assertEquals(HttpStatus.OK, ok.getStatusCode());
        assertEquals("DELETED", s1.getStatus());

        // Deleted session is filtered out from getSessions
        ResponseEntity<?> listResp = chatController.getSessions(studentToken, null);
        List<?> activeSessions = (List<?>) listResp.getBody();
        assertFalse(activeSessions.stream().anyMatch(s -> ((ChatSession)s).getId().equals(40L)));
    }

    @Test
    void testUpdateStatus_QueryParamAndBody_BothSucceed() {
        ChatSession s1 = new ChatSession();
        s1.setUserEmail("student1@vrsec.ac.in");
        s1.setUser(userDb.get("student1@vrsec.ac.in"));
        s1.setTitle("Session to Update");
        s1.setStatus("ACTIVE");
        s1.setId(45L);
        sessionDb.put(45L, s1);

        // 1. Query parameter update
        ResponseEntity<?> resp1 = chatController.updateStatus(studentToken, 45L, "ARCHIVED", null);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertEquals("ARCHIVED", s1.getStatus());

        // 2. Request body update
        ResponseEntity<?> resp2 = chatController.updateStatus(studentToken, 45L, null, Map.of("status", "COMPLETED"));
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        assertEquals("COMPLETED", s1.getStatus());

        // 3. Unauthorized access by Student 2 -> 403
        ResponseEntity<?> resp3 = chatController.updateStatus(student2Token, 45L, "ACTIVE", null);
        assertEquals(HttpStatus.FORBIDDEN, resp3.getStatusCode());
    }

    @Test
    void testGetAiStatus_AuthenticatedUser_ReturnsEngineInfo() {
        ResponseEntity<?> resp = chatController.getAiStatus(studentToken);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("activeEngine"));
        assertTrue(body.containsKey("groqModel"));
        assertEquals("openai/gpt-oss-120b", body.get("groqModel"));
    }

    @Test
    void testAdminUsage_AccessControl() {
        // Admin gets 200 OK
        ResponseEntity<?> adminResp = chatController.getAdminUsage(adminToken);
        assertEquals(HttpStatus.OK, adminResp.getStatusCode());
        assertNotNull(adminResp.getBody());

        // Student gets 403 Forbidden
        ResponseEntity<?> studentResp = chatController.getAdminUsage(studentToken);
        assertEquals(HttpStatus.FORBIDDEN, studentResp.getStatusCode());
    }

    @Test
    void testSendMessage_NonExistentSessionId_Returns404() {
        Map<String, Object> req = Map.of("sessionId", 99999L, "message", "Test non-existent session");
        ResponseEntity<?> resp = chatController.sendMessage(studentToken, req);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // =========================================================================
    // 3. STUDENT CONTEXT & DOMAIN FILTERING TESTS
    // =========================================================================
    @Test
    void testStudentContext_IncludesAccurateCoursesAndPhases() {
        User student = userDb.get("student1@vrsec.ac.in");
        List<Course> courses = courseDb.values().stream()
                .filter(c -> c.getUserEmail().equalsIgnoreCase(student.getEmail()))
                .collect(Collectors.toList());
        List<QuizAttempt> attempts = quizAttemptDb;
        List<TopicPerformance> weakTopics = topicPerformanceDb;

        String context = ReflectionTestUtils.invokeMethod(aiAssistantService, "buildStudentContext",
                student, courses, attempts, weakTopics);

        assertNotNull(context);
        assertTrue(context.contains("Database Management Systems"));
        assertTrue(context.contains("Progress: 50%"));
        assertTrue(context.contains("Phase 1: ER Modeling & Relational Algebra [COMPLETED]"));
        assertTrue(context.contains("Phase 2: Normalization: 1NF, 2NF, 3NF & BCNF [IN PROGRESS]"));
        assertTrue(context.contains("CGPA: 8.4 / 10.0"));
        assertTrue(context.contains("Transitive Dependencies in 3NF"));
    }

    @Test
    void testFacultyContext_IncludesStudentsWithNonInstitutionalEmailDomains() {
        // Faculty in CSE should see student1, student2, AND student.external@gmail.com
        User faculty = userDb.get("faculty.cse@siddhartha.ac.in");
        ChatSession session = new ChatSession();
        session.setId(50L);
        session.setUserEmail(faculty.getEmail());
        sessionDb.put(50L, session);

        Map<String, Object> result = aiAssistantService.chatWithFacultyAi(faculty, session, "Summarize student performance");
        assertNotNull(result);

        // Verify that student.external@gmail.com is included in CSE department count
        long cseStudentCount = userDb.values().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> "CSE".equalsIgnoreCase(u.getBranch()))
                .count();
        assertEquals(3, cseStudentCount, "All 3 CSE students (including Gmail) must be included");
    }

    // =========================================================================
    // 4. SEPARATION OF AUTHORIZATION DUTIES
    // =========================================================================
    @Test
    void testSeparationOfDuties_CourseApproverDoesNotGetInstitutionWideAccess() {
        // HOD CSE is a designated course approver, but their department scope is strictly CSE
        User hodCSE = userDb.get("hod.cse@siddhartha.ac.in");
        ChatSession session = new ChatSession();
        session.setId(55L);
        session.setUserEmail(hodCSE.getEmail());
        sessionDb.put(55L, session);

        Map<String, Object> result = aiAssistantService.chatWithFacultyAi(hodCSE, session, "Summarize cohort analytics");
        assertNotNull(result);

        // Department scope remains CSE, MECH students are NOT included
        long cseCount = userDb.values().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> "CSE".equalsIgnoreCase(u.getBranch()))
                .count();
        assertEquals(3, cseCount);

        long mechCount = userDb.values().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> "MECH".equalsIgnoreCase(u.getBranch()))
                .count();
        assertEquals(1, mechCount);
    }

    @Test
    void testSeparationOfDuties_AdminReceivesInstitutionWideAccess() {
        // Admin gets all departments
        User admin = userDb.get("admin@siddhartha.ac.in");
        ChatSession session = new ChatSession();
        session.setId(56L);
        session.setUserEmail(admin.getEmail());
        sessionDb.put(56L, session);

        Map<String, Object> result = aiAssistantService.chatWithFacultyAi(admin, session, "Overall university report");
        assertNotNull(result);

        long totalStudents = userDb.values().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .count();
        assertEquals(4, totalStudents, "Admin must be authorized for all 4 students across all departments");
    }

    @Test
    void testFacultyScoping_OrdinaryFacultyCannotAccessOtherDepartments() {
        // MECH faculty should only see the 1 MECH student, never CSE students
        User mechFaculty = userDb.get("faculty.mech@siddhartha.ac.in");
        List<User> students = userDb.values().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> "MECH".equalsIgnoreCase(u.getBranch()))
                .collect(Collectors.toList());

        assertEquals(1, students.size(), "MECH faculty must only access MECH students");
        assertEquals("mech.student@outlook.com", students.get(0).getEmail());
    }

    @Test
    void testFacultyAi_StudentAccountRejectedWithSecurityException() {
        User student = userDb.get("student1@vrsec.ac.in");
        ChatSession session = new ChatSession();
        session.setId(60L);
        session.setUserEmail(student.getEmail());

        assertThrows(SecurityException.class, () -> {
            aiAssistantService.chatWithFacultyAi(student, session, "Give me faculty analytics");
        });
    }

    // =========================================================================
    // 5. CONVERSATION HISTORY & MULTI-TURN CONTENTS TESTS
    // =========================================================================
    @Test
    void testMultiTurnContents_MaintainsAlternatingUserAndModelSequence() {
        ChatSession session = new ChatSession();
        session.setId(70L);

        List<ChatMessage> past = new ArrayList<>();
        past.add(new ChatMessage(session, "user", "What is 2NF?"));
        past.add(new ChatMessage(session, "ai", "2NF eliminates partial dependency on composite keys."));

        List<Map<String, Object>> contents = ReflectionTestUtils.invokeMethod(
                aiAssistantService, "buildConversationContents", past, "Now compare it with 3NF");

        assertNotNull(contents);
        assertEquals(3, contents.size());

        assertEquals("user", contents.get(0).get("role"));
        assertEquals("What is 2NF?", contents.get(0).get("content"));

        assertEquals("assistant", contents.get(1).get("role"));
        assertTrue(((String) contents.get(1).get("content")).contains("2NF eliminates"));

        assertEquals("user", contents.get(2).get("role"));
        assertEquals("Now compare it with 3NF", contents.get(2).get("content"));
    }

    // =========================================================================
    // 6. OPENAI RESPONSES API PARSING & RESILIENT EXTRACTION
    // =========================================================================
    @Test
    void testOpenAiResponseExtraction_StandardResponsesApiFormat() {
        Map<String, Object> sample = Map.of(
                "id", "resp_12345",
                "object", "response",
                "status", "completed",
                "model", "gpt-4o-mini",
                "output", List.of(
                        Map.of(
                                "id", "msg_123",
                                "type", "message",
                                "role", "assistant",
                                "content", List.of(
                                        Map.of("type", "output_text", "text", "OpenAI response successfully parsed.")
                                )
                        )
                )
        );

        String extracted = AiAssistantService.extractTextFromOpenAiResponse(sample);
        assertEquals("OpenAI response successfully parsed.", extracted);
    }

    @Test
    void testOpenAiResponseExtraction_TopLevelOutputTextFormat() {
        Map<String, Object> sample = Map.of(
                "id", "resp_67890",
                "output_text", "Direct top-level output text."
        );

        String extracted = AiAssistantService.extractTextFromOpenAiResponse(sample);
        assertEquals("Direct top-level output text.", extracted);
    }

    @Test
    void testGroqResponseExtraction_StandardChatCompletionsFormat() {
        Map<String, Object> sample = Map.of(
                "id", "chatcmpl-test-groq-12345",
                "object", "chat.completion",
                "created", 1729000000,
                "model", "openai/gpt-oss-120b",
                "choices", List.of(
                        Map.of(
                                "index", 0,
                                "message", Map.of(
                                        "role", "assistant",
                                        "content", "Groq openai/gpt-oss-120b response successfully parsed."
                                ),
                                "finish_reason", "stop"
                        )
                )
        );

        String extracted = AiAssistantService.extractTextFromOpenAiResponse(sample);
        assertEquals("Groq openai/gpt-oss-120b response successfully parsed.", extracted);
    }

    @Test
    void testOpenAiResponseExtraction_LegacyChatCompletionsFallback() {
        Map<String, Object> sample = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("role", "assistant", "content", "Fallback chat completion content."))
                )
        );

        String extracted = AiAssistantService.extractTextFromOpenAiResponse(sample);
        assertEquals("Fallback chat completion content.", extracted);
    }

    // =========================================================================
    // 7. AI ERROR HANDLING & ZERO FAKE STATIC FALLBACKS
    // =========================================================================
    @Test
    void testAiOffline_ReturnsRetryableError_WithoutFakeStaticFallback() {
        User student = userDb.get("student1@vrsec.ac.in");
        ChatSession session = new ChatSession();
        session.setId(80L);
        session.setUserEmail(student.getEmail());
        sessionDb.put(80L, session);

        Map<String, Object> result = aiAssistantService.chatWithStudentAi(student, session, "What is the capital of Japan?");

        assertNotNull(result);
        assertTrue((Boolean) result.get("isError"), "Should report error when AI key is not configured or offline");
        String reply = (String) result.get("reply");
        assertTrue(reply.contains("temporarily unavailable"), "Should return transparent retryable error");
        assertFalse(reply.contains("Here is your real-time learning summary"), "Must NEVER return hardcoded static fallback");

        List<ChatMessage> savedAi = messageDb.stream()
                .filter(m -> "ai".equalsIgnoreCase(m.getSender()) && m.getSession().getId().equals(80L))
                .collect(Collectors.toList());
        assertEquals(0, savedAi.size(), "Transient errors must NOT be saved into chat history");
    }
}
