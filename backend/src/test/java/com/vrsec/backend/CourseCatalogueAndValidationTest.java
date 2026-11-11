package com.vrsec.backend;

import com.vrsec.backend.controller.ApiController;
import com.vrsec.backend.controller.QuizController;
import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import com.vrsec.backend.service.AuthService;
import com.vrsec.backend.service.JwtService;
import com.vrsec.backend.service.LearningPathService;
import com.vrsec.backend.service.SubjectCurriculumRegistry;
import com.vrsec.backend.service.SubjectQuestionBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class CourseCatalogueAndValidationTest {

    private ApiController apiController;
    private QuizController quizController;
    private LearningPathService learningPathService;
    private JwtService jwtService;

    private Map<Long, Course> courseDb;
    private Map<Long, CoursePhase> phaseDb;
    private Map<Long, List<QuizQuestion>> questionDb;
    private Map<String, User> userDb;
    private Map<Long, ApprovedCourse> approvedCourseDb;
    private Map<Long, CourseRequest> courseRequestDb;
    private CourseRequestRepository courseRequestRepository;
    private QuizQuestionRepository questionRepository;

    @BeforeEach
    void setUp() {
        courseDb = new ConcurrentHashMap<>();
        phaseDb = new ConcurrentHashMap<>();
        questionDb = new ConcurrentHashMap<>();
        userDb = new ConcurrentHashMap<>();
        approvedCourseDb = new ConcurrentHashMap<>();
        courseRequestDb = new ConcurrentHashMap<>();

        learningPathService = new LearningPathService();
        apiController = new ApiController();

        // 1. CourseRepository Mock
        CourseRepository courseRepository = (CourseRepository) Proxy.newProxyInstance(
                CourseRepository.class.getClassLoader(),
                new Class<?>[]{CourseRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        Course c = (Course) args[0];
                        if (c.getId() == null) c.setId((long) (courseDb.size() + 1));
                        courseDb.put(c.getId(), c);
                        return c;
                    }
                    if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(courseDb.get(id));
                    }
                    if ("findByUserEmailIgnoreCaseAndDeletedFalse".equals(name) || "findByUserEmailIgnoreCase".equals(name)) {
                        String email = (String) args[0];
                        List<Course> list = new ArrayList<>();
                        for (Course c : courseDb.values()) {
                            if (email != null && email.equalsIgnoreCase(c.getUserEmail())) {
                                if ("findByUserEmailIgnoreCaseAndDeletedFalse".equals(name) && c.isDeleted()) continue;
                                list.add(c);
                            }
                        }
                        return list;
                    }
                    return null;
                }
        );

        // 2. CoursePhaseRepository Mock
        CoursePhaseRepository phaseRepository = (CoursePhaseRepository) Proxy.newProxyInstance(
                CoursePhaseRepository.class.getClassLoader(),
                new Class<?>[]{CoursePhaseRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        CoursePhase cp = (CoursePhase) args[0];
                        if (cp.getId() == null) cp.setId((long) (phaseDb.size() + 1));
                        phaseDb.put(cp.getId(), cp);
                        return cp;
                    }
                    if ("findByCourseIdAndPhaseNumber".equals(name)) {
                        Long cid = (Long) args[0];
                        Integer pnum = (Integer) args[1];
                        for (CoursePhase cp : phaseDb.values()) {
                            if (cp.getCourse() != null && Objects.equals(cp.getCourse().getId(), cid)
                                    && Objects.equals(cp.getPhaseNumber(), pnum)) {
                                return Optional.of(cp);
                            }
                        }
                        return Optional.empty();
                    }
                    if ("findByCourseIdOrderByPhaseNumberAsc".equals(name)) {
                        Long cid = (Long) args[0];
                        List<CoursePhase> list = new ArrayList<>();
                        for (CoursePhase cp : phaseDb.values()) {
                            if (cp.getCourse() != null && Objects.equals(cp.getCourse().getId(), cid)) {
                                list.add(cp);
                            }
                        }
                        list.sort(Comparator.comparing(CoursePhase::getPhaseNumber));
                        return list;
                    }
                    return null;
                }
        );

        // 3. QuizQuestionRepository Mock
        questionRepository = (QuizQuestionRepository) Proxy.newProxyInstance(
                QuizQuestionRepository.class.getClassLoader(),
                new Class<?>[]{QuizQuestionRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("saveAll".equals(name)) {
                        List<QuizQuestion> list = (List<QuizQuestion>) args[0];
                        long base = questionDb.size() + 1;
                        for (QuizQuestion q : list) {
                            if (q.getId() == null) q.setId(base++);
                            Long phaseId = q.getPhase() != null ? q.getPhase().getId() : 0L;
                            questionDb.computeIfAbsent(phaseId, k -> new ArrayList<>()).add(q);
                        }
                        return list;
                    }
                    if ("findByPhaseIdOrderByIdAsc".equals(name)) {
                        Long pid = (Long) args[0];
                        return new ArrayList<>(questionDb.getOrDefault(pid, Collections.emptyList()));
                    }
                    if ("deleteAll".equals(name) || "flush".equals(name)) {
                        return null;
                    }
                    return null;
                }
        );

        // 4. UserRepository Mock
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByEmail".equals(name) || "findByEmailIgnoreCase".equals(name)) {
                        String email = (String) args[0];
                        return Optional.ofNullable(userDb.get(email != null ? email.toLowerCase() : null));
                    }
                    return null;
                }
        );

        // 5. ApprovedCourseRepository Mock
        ApprovedCourseRepository approvedCourseRepository = (ApprovedCourseRepository) Proxy.newProxyInstance(
                ApprovedCourseRepository.class.getClassLoader(),
                new Class<?>[]{ApprovedCourseRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("count".equals(name)) {
                        return (long) approvedCourseDb.size();
                    }
                    if ("save".equals(name)) {
                        ApprovedCourse ac = (ApprovedCourse) args[0];
                        if (ac.getId() == null) ac.setId((long) (approvedCourseDb.size() + 1));
                        approvedCourseDb.put(ac.getId(), ac);
                        return ac;
                    }
                    if ("saveAll".equals(name)) {
                        List<ApprovedCourse> list = (List<ApprovedCourse>) args[0];
                        for (ApprovedCourse ac : list) {
                            if (ac.getId() == null) ac.setId((long) (approvedCourseDb.size() + 1));
                            approvedCourseDb.put(ac.getId(), ac);
                        }
                        return list;
                    }
                    if ("findAllByActiveTrueOrderByCategoryAscTitleAsc".equals(name)
                            || "findByActiveTrueOrderByTitleAsc".equals(name)
                            || "findByActiveTrue".equals(name)
                            || "findAll".equals(name)) {
                        List<ApprovedCourse> list = new ArrayList<>();
                        for (ApprovedCourse ac : approvedCourseDb.values()) {
                            if (ac.isActive()) list.add(ac);
                        }
                        return list;
                    }
                    if ("findByIdAndActiveTrue".equals(name) || "findById".equals(name)) {
                        Long id = (Long) args[0];
                        ApprovedCourse ac = approvedCourseDb.get(id);
                        return (ac != null && ac.isActive()) ? Optional.of(ac) : Optional.empty();
                    }
                    if ("findByTitleIgnoreCaseAndActiveTrue".equals(name)) {
                        String title = (String) args[0];
                        for (ApprovedCourse ac : approvedCourseDb.values()) {
                            if (ac.isActive() && ac.getTitle().equalsIgnoreCase(title)) {
                                return Optional.of(ac);
                            }
                        }
                        return Optional.empty();
                    }
                    if ("findByCodeIgnoreCaseAndActiveTrue".equals(name)) {
                        String code = (String) args[0];
                        for (ApprovedCourse ac : approvedCourseDb.values()) {
                            if (ac.isActive() && code.equalsIgnoreCase(ac.getCode())) {
                                return Optional.of(ac);
                            }
                        }
                        return Optional.empty();
                    }
                    if ("existsByTitleIgnoreCaseAndActiveTrue".equals(name)) {
                        String title = (String) args[0];
                        for (ApprovedCourse ac : approvedCourseDb.values()) {
                            if (ac.isActive() && ac.getTitle().equalsIgnoreCase(title)) {
                                return true;
                            }
                        }
                        return false;
                    }
                    return null;
                }
        );

        // 6. CourseRequestRepository Mock
        courseRequestRepository = (CourseRequestRepository) Proxy.newProxyInstance(
                CourseRequestRepository.class.getClassLoader(),
                new Class<?>[]{CourseRequestRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        CourseRequest cr = (CourseRequest) args[0];
                        if (cr.getId() == null) cr.setId((long) (courseRequestDb.size() + 1));
                        courseRequestDb.put(cr.getId(), cr);
                        return cr;
                    }
                    if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(courseRequestDb.get(id));
                    }
                    if ("findByStudentEmailIgnoreCaseOrderByCreatedAtDesc".equals(name)) {
                        String email = (String) args[0];
                        List<CourseRequest> list = new ArrayList<>();
                        for (CourseRequest cr : courseRequestDb.values()) {
                            if (email != null && email.equalsIgnoreCase(cr.getStudentEmail())) {
                                list.add(cr);
                            }
                        }
                        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                        return list;
                    }
                    if ("findAllByOrderByCreatedAtDesc".equals(name)) {
                        List<CourseRequest> list = new ArrayList<>(courseRequestDb.values());
                        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                        return list;
                    }
                    return null;
                }
        );

        // 7. SuggestedCourse & TopicPerformance Mocks
        SuggestedCourseRepository suggestedCourseRepository = (SuggestedCourseRepository) Proxy.newProxyInstance(
                SuggestedCourseRepository.class.getClassLoader(),
                new Class<?>[]{SuggestedCourseRepository.class},
                (proxy, method, args) -> Collections.emptyList()
        );
        TopicPerformanceRepository topicPerformanceRepository = (TopicPerformanceRepository) Proxy.newProxyInstance(
                TopicPerformanceRepository.class.getClassLoader(),
                new Class<?>[]{TopicPerformanceRepository.class},
                (proxy, method, args) -> Collections.emptyList()
        );
        QuizAttemptRepository quizAttemptRepository = (QuizAttemptRepository) Proxy.newProxyInstance(
                QuizAttemptRepository.class.getClassLoader(),
                new Class<?>[]{QuizAttemptRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    return Collections.emptyList();
                }
        );

        // Wire LearningPathService dependencies
        ReflectionTestUtils.setField(learningPathService, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(learningPathService, "phaseRepository", phaseRepository);
        ReflectionTestUtils.setField(learningPathService, "questionRepository", questionRepository);
        ReflectionTestUtils.setField(learningPathService, "userRepository", userRepository);
        ReflectionTestUtils.setField(learningPathService, "suggestedCourseRepository", suggestedCourseRepository);
        ReflectionTestUtils.setField(learningPathService, "topicPerformanceRepository", topicPerformanceRepository);
        ReflectionTestUtils.setField(learningPathService, "approvedCourseRepository", approvedCourseRepository);
        ReflectionTestUtils.setField(learningPathService, "attemptRepository", quizAttemptRepository);

        // Wire ApiController dependencies
        ReflectionTestUtils.setField(apiController, "userRepository", userRepository);
        ReflectionTestUtils.setField(apiController, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(apiController, "phaseRepository", phaseRepository);
        ReflectionTestUtils.setField(apiController, "approvedCourseRepository", approvedCourseRepository);
        ReflectionTestUtils.setField(apiController, "courseRequestRepository", courseRequestRepository);
        ReflectionTestUtils.setField(apiController, "topicPerformanceRepository", topicPerformanceRepository);
        ReflectionTestUtils.setField(apiController, "quizAttemptRepository", quizAttemptRepository);
        ReflectionTestUtils.setField(apiController, "learningPathService", learningPathService);

        AuthService authService = new AuthService();
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(apiController, "authService", authService);

        jwtService = new JwtService();
        jwtService.init();
        ReflectionTestUtils.setField(apiController, "jwtService", jwtService);

        // Wire QuizController dependencies
        quizController = new QuizController();
        ReflectionTestUtils.setField(quizController, "learningPathService", learningPathService);
        ReflectionTestUtils.setField(quizController, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(quizController, "phaseRepository", phaseRepository);
        ReflectionTestUtils.setField(quizController, "userRepository", userRepository);
        ReflectionTestUtils.setField(quizController, "jwtService", jwtService);

        // Initialize approved catalogue in mock db
        learningPathService.syncApprovedCatalogue();
    }

    private User createFaculty(String email, String name, String branch) {
        User u = new User();
        u.setId((long) (userDb.size() + 1));
        u.setUsername(name);
        u.setEmail(email.toLowerCase());
        u.setRole("FACULTY");
        u.setBranch(branch);
        userDb.put(email.toLowerCase(), u);
        return u;
    }

    private User createStudent(String email, String branch, String year, String sem) {
        User u = new User();
        u.setId((long) (userDb.size() + 1));
        u.setUsername("Student " + email.split("@")[0]);
        u.setEmail(email.toLowerCase());
        u.setRole("STUDENT");
        u.setBranch(branch);
        u.setAcademicYear(year);
        u.setSemester(sem);
        u.setCgpa(8.5);
        userDb.put(email.toLowerCase(), u);
        return u;
    }

    // =========================================================================
    // TEST 1: Approved Catalogue Synchronization & Integrity
    // =========================================================================
    @Test
    @DisplayName("Test 1: Approved course catalogue sync initializes approved courses across branches")
    void test1_ApprovedCatalogueSyncAndIntegrity() {
        assertTrue(approvedCourseDb.size() >= 12, "Catalogue should contain at least 12 standard engineering courses");

        // Verify Data Structures exists
        boolean hasDsa = approvedCourseDb.values().stream()
                .anyMatch(c -> c.getTitle().contains("Data Structures") && "CSE".equalsIgnoreCase(c.getBranch()));
        assertTrue(hasDsa, "Data Structures for CSE must be present");

        // Verify Shared Foundations exist
        boolean hasMath = approvedCourseDb.values().stream()
                .anyMatch(c -> c.getTitle().contains("Mathematics") && "ALL".equalsIgnoreCase(c.getBranch()));
        assertTrue(hasMath, "Engineering Mathematics shared foundation must be present");

        // Verify Non-Computing subjects exist
        boolean hasFluid = approvedCourseDb.values().stream()
                .anyMatch(c -> c.getTitle().contains("Fluid Mechanics") && "MECH".equalsIgnoreCase(c.getBranch()));
        assertTrue(hasFluid, "Fluid Mechanics for MECH must be present");
    }

    // =========================================================================
    // TEST 2: Catalogue Filtering by Branch and Academic Year (Priority 1)
    // =========================================================================
    @Test
    @DisplayName("Test 2: Catalogue filters properly by student branch, year and includes shared foundations")
    void test2_CatalogueBranchAndYearEligibility() {
        createStudent("cse.student@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");

        ResponseEntity<?> response = apiController.getCourseCatalogue(
                "cse.student@vrsec.ac.in", "CSE", "2nd Year", "1st Sem", null, null, "eligible");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Map<String, Object>> list = (List<Map<String, Object>>) response.getBody();
        assertNotNull(list);
        assertFalse(list.isEmpty());

        for (Map<String, Object> c : list) {
            String b = (String) c.get("branch");
            boolean isCseOrShared = "CSE".equalsIgnoreCase(b) || "ALL".equalsIgnoreCase(b) || "SHARED".equalsIgnoreCase(b);
            assertTrue(isCseOrShared, "Eligible catalogue for CSE student should only contain CSE or SHARED courses, found: " + b);
        }
    }

    // =========================================================================
    // TEST 3: CRITICAL - Rejection of Arbitrary Text and Personal Names (Priority 1)
    // =========================================================================
    @Test
    @DisplayName("Test 3: System strictly rejects arbitrary text, personal names, and unapproved courses with HTTP 400")
    void test3_StrictRejectionOfArbitraryTextAndPersonalNames() {
        createStudent("student@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        String studentToken = "Bearer " + jwtService.generateToken("student@vrsec.ac.in", "STUDENT", 1L);

        String[] invalidInputs = {
                "John Doe",
                "Vivek Varanasi",
                "Random Unapproved Phrase",
                "Hacking 101",
                "My Custom Subject",
                "Some Random String 12345",
                "Machine Learning in 24 Hours Without Math"
        };

        for (String invalid : invalidInputs) {
            Map<String, Object> req = new HashMap<>();
            req.put("email", "student@vrsec.ac.in");
            req.put("subject", invalid);

            ResponseEntity<?> res = apiController.selectOrGenerateCourse(studentToken, req);
            assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode(),
                    "Arbitrary string '" + invalid + "' MUST be rejected with HTTP 400");

            Map<String, Object> body = (Map<String, Object>) res.getBody();
            assertNotNull(body);
            assertTrue(body.containsKey("error"));
            assertTrue(body.get("error").toString().contains("not in the approved catalogue"),
                    "Error message should explain that course is not in approved catalogue");
        }

        // Verify zero courses were inserted or created
        assertEquals(0, courseDb.size(), "No courses should be created for rejected arbitrary inputs");
    }

    // =========================================================================
    // TEST 4: Enrollment in Genuine Approved Course Succeeds (Priority 1 & 2)
    // =========================================================================
    @Test
    @DisplayName("Test 4: Enrolling in an approved course succeeds and generates authentic learning path")
    void test4_EnrollmentInApprovedCourseSucceeds() {
        createStudent("student@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        String studentToken = "Bearer " + jwtService.generateToken("student@vrsec.ac.in", "STUDENT", 1L);

        Map<String, Object> req = new HashMap<>();
        req.put("email", "student@vrsec.ac.in");
        req.put("subject", "Data Structures & Algorithmic Complexity");

        ResponseEntity<?> res = apiController.selectOrGenerateCourse(studentToken, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("Data Structures & Algorithmic Complexity", body.get("title"));
        assertEquals(8, body.get("totalPhases"));

        assertEquals(1, courseDb.size(), "One enrolled course should be saved in database");
    }

    // =========================================================================
    // TEST 5: Course Request Submission Stores PENDING without Generating Path
    // =========================================================================
    @Test
    @DisplayName("Test 5: Course Request stores PENDING status and does NOT generate a learning path")
    void test5_CourseRequestStoresPendingWithoutGeneratingPath() {
        createStudent("student@vrsec.ac.in", "CSE", "3rd Year", "1st Sem");

        Map<String, String> req = new HashMap<>();
        req.put("email", "student@vrsec.ac.in");
        req.put("title", "Quantum Computing & Information");
        req.put("reason", "Preparation for research elective");

        String studentToken = "Bearer " + jwtService.generateToken("student@vrsec.ac.in", "STUDENT", 1L);
        ResponseEntity<?> res = apiController.requestCourse(studentToken, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("PENDING", body.get("status"));

        // CRITICAL CHECK: Verify that NO course or learning path was created for this pending request!
        assertEquals(0, courseDb.size(), "No course should be created for a pending request!");
        assertEquals(1, courseRequestDb.size(), "Request should be recorded in course requests table");

        CourseRequest cr = courseRequestDb.values().iterator().next();
        assertEquals("PENDING", cr.getStatus());
        assertEquals("Quantum Computing & Information", cr.getSuggestedTitle());
    }

    // =========================================================================
    // TEST 6: Duplicate Pending Course Request is Rejected
    // =========================================================================
    @Test
    @DisplayName("Test 6: Duplicate pending course request by same student is rejected with HTTP 400")
    void test6_DuplicatePendingCourseRequestRejected() {
        createStudent("student@vrsec.ac.in", "CSE", "3rd Year", "1st Sem");

        Map<String, String> req = new HashMap<>();
        req.put("email", "student@vrsec.ac.in");
        req.put("title", "Parallel Computing");
        req.put("reason", "Elective study");

        String studentToken = "Bearer " + jwtService.generateToken("student@vrsec.ac.in", "STUDENT", 1L);
        ResponseEntity<?> firstRes = apiController.requestCourse(studentToken, req);
        assertEquals(HttpStatus.OK, firstRes.getStatusCode());

        // Attempt duplicate request
        ResponseEntity<?> secondRes = apiController.requestCourse(studentToken, req);
        assertEquals(HttpStatus.BAD_REQUEST, secondRes.getStatusCode());

        Map<String, Object> body = (Map<String, Object>) secondRes.getBody();
        assertTrue(body.get("error").toString().contains("already have a pending request"));
    }

    // =========================================================================
    // TEST 7: Faculty Approval Enables Course Enrollment in Catalogue
    // =========================================================================
    @Test
    @DisplayName("Test 7: Faculty approval flow marks request APPROVED and enables course enrollment")
    void test7_FacultyApprovalFlowEnablesEnrollment() {
        createStudent("student@vrsec.ac.in", "CSE", "3rd Year", "1st Sem");

        // 1. Submit request
        Map<String, String> req = new HashMap<>();
        req.put("email", "student@vrsec.ac.in");
        req.put("title", "Distributed Systems & Cloud Architecture");
        req.put("reason", "Industry preparation");
        String studentToken = "Bearer " + jwtService.generateToken("student@vrsec.ac.in", "STUDENT", 1L);
        apiController.requestCourse(studentToken, req);

        CourseRequest pendingReq = courseRequestDb.values().iterator().next();
        Long requestId = pendingReq.getId();

        // 2a. SECURITY CHECK: Student attempts to approve course request -> MUST BE REJECTED WITH 403 FORBIDDEN
        Map<String, Object> unauthorizedPayload = new HashMap<>();
        unauthorizedPayload.put("requestId", requestId);
        unauthorizedPayload.put("approverEmail", "hod.cse@siddhartha.ac.in"); // Forged email in body!
        ResponseEntity<?> unauthorizedRes = apiController.approveCourseRequest(studentToken, unauthorizedPayload);
        assertEquals(HttpStatus.FORBIDDEN, unauthorizedRes.getStatusCode(),
                "Students must NEVER be allowed to approve course requests, even if forging faculty email in body!");
        assertEquals("PENDING", pendingReq.getStatus(), "Request must remain PENDING after unauthorized attempt");

        // 2b. Authorized Faculty approves request
        createFaculty("faculty@siddhartha.ac.in", "Prof. Sharma", "CSE");
        String facultyToken = "Bearer " + jwtService.generateToken("faculty@siddhartha.ac.in", "FACULTY", 99L);
        Map<String, Object> approvePayload = new HashMap<>();
        approvePayload.put("requestId", requestId);
        approvePayload.put("branch", "CSE");
        approvePayload.put("academicYear", "3rd Year");
        approvePayload.put("semester", "1st Sem");
        approvePayload.put("category", "ELECTIVE");

        ResponseEntity<?> approveRes = apiController.approveCourseRequest(facultyToken, approvePayload);
        assertEquals(HttpStatus.OK, approveRes.getStatusCode());

        // Verify request updated
        assertEquals("APPROVED", pendingReq.getStatus());
        assertEquals("Prof. Sharma", pendingReq.getReviewedBy());

        // Verify new course in approved catalogue
        boolean inCatalogue = approvedCourseDb.values().stream()
                .anyMatch(c -> c.getTitle().equalsIgnoreCase("Distributed Systems & Cloud Architecture"));
        assertTrue(inCatalogue, "Approved course must now exist in approved catalogue");

        // 3. Student can now enroll in newly approved course!
        Map<String, Object> enrollPayload = new HashMap<>();
        enrollPayload.put("email", "student@vrsec.ac.in");
        enrollPayload.put("subject", "Distributed Systems & Cloud Architecture");

        ResponseEntity<?> enrollRes = apiController.selectOrGenerateCourse(studentToken, enrollPayload);
        assertEquals(HttpStatus.OK, enrollRes.getStatusCode());
        assertEquals(1, courseDb.size());

        // 4. Ineligible student enrollment check: A Mechanical student attempting to enroll in an ECE Core course
        User mechStudent = createStudent("mech.student@vrsec.ac.in", "MECH", "2nd Year", "1st Sem");
        String mechToken = "Bearer " + jwtService.generateToken("mech.student@vrsec.ac.in", "STUDENT", 2L);
        Map<String, Object> ineligiblePayload = new HashMap<>();
        ineligiblePayload.put("email", "mech.student@vrsec.ac.in");
        ineligiblePayload.put("subject", "Electronic Devices & Circuit Theory"); // ECE CORE course

        ResponseEntity<?> ineligibleRes = apiController.selectOrGenerateCourse(mechToken, ineligiblePayload);
        assertEquals(HttpStatus.BAD_REQUEST, ineligibleRes.getStatusCode(),
                "Cross-department enrollment in restricted core course must fail eligibility check!");
        Map<String, Object> errorBody = (Map<String, Object>) ineligibleRes.getBody();
        assertTrue(errorBody.get("error").toString().contains("Academic eligibility check failed"),
                "Error should explicitly indicate academic eligibility failure");
    }

    // =========================================================================
    // TEST 8: Domain Isolation - Non-Computing Courses Have Zero Coding Content (Priority 2)
    // =========================================================================
    @Test
    @DisplayName("Test 8: Non-computing courses (Fluid Mechanics, Environmental Science) have zero coding content")
    void test8_NonComputingCoursesHaveZeroCodingContent() {
        User mechStudent = createStudent("mech@vrsec.ac.in", "MECH", "2nd Year", "1st Sem");
        List<Map<String, Object>> fluidPhases = SubjectCurriculumRegistry.getCurriculumForSubject("Fluid Mechanics & Hydraulic Machinery", mechStudent);

        assertNotNull(fluidPhases);
        assertEquals(6, fluidPhases.size());

        String[] codingKeywords = {"syntax", "def ", "class ", "void ", "function()", "import ", "npm", "gcc", "compile"};
        for (Map<String, Object> p : fluidPhases) {
            String title = (String) p.get("title");
            String desc = (String) p.get("description");
            for (String kw : codingKeywords) {
                assertFalse(title.toLowerCase().contains(kw), "Coding keyword found in Fluid Mechanics title: " + kw);
                if (desc != null) {
                    assertFalse(desc.toLowerCase().contains(kw), "Coding keyword found in Fluid Mechanics description: " + kw);
                }
            }
        }

        // Check Environmental Science
        List<Map<String, Object>> envPhases = SubjectCurriculumRegistry.getCurriculumForSubject("Environmental Science & Engineering / Sustainability", mechStudent);
        assertNotNull(envPhases);
        assertEquals(6, envPhases.size());
        for (Map<String, Object> p : envPhases) {
            String title = (String) p.get("title");
            for (String kw : codingKeywords) {
                assertFalse(title.toLowerCase().contains(kw), "Coding keyword in Env Science title: " + kw);
            }
        }
    }

    // =========================================================================
    // TEST 9: Quiz Strict Rules: 10 Questions, 4 Options, Balanced Distribution (Priority 3)
    // =========================================================================
    @Test
    @DisplayName("Test 9: Quiz engines produce strictly 10 questions with 4 distinct options and valid answers")
    void test9_QuizStrictRulesVerification() {
        User student = createStudent("quiz.tester@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");

        String[] subjectsToTest = {
                "Data Structures & Algorithms",
                "Engineering Mathematics & Calculus",
                "Digital Logic Design & Digital Electronics",
                "Fluid Mechanics & Hydraulic Machinery",
                "Environmental Science & Engineering / Sustainability"
        };

        for (String subject : subjectsToTest) {
            Course course = new Course();
            course.setTitle(subject);
            CoursePhase phase = new CoursePhase();
            phase.setCourse(course);
            phase.setPhaseNumber(1);

            List<QuizQuestion> questions = SubjectQuestionBank.getQuestionsForPhase(phase, student);
            assertNotNull(questions, "Questions for " + subject + " should not be null");
            assertEquals(10, questions.size(), "Subject " + subject + " must have exactly 10 questions");

            Map<String, Integer> answerDistribution = new HashMap<>();

            for (int i = 0; i < questions.size(); i++) {
                QuizQuestion q = questions.get(i);
                String prompt = q.getQuestionText();
                assertNotNull(prompt, "Question prompt cannot be null");
                assertFalse(prompt.trim().isEmpty(), "Question prompt cannot be empty");

                String optA = q.getOptionA();
                String optB = q.getOptionB();
                String optC = q.getOptionC();
                String optD = q.getOptionD();

                assertNotNull(optA, "Option A cannot be null in " + subject + " Q" + (i + 1));
                assertNotNull(optB, "Option B cannot be null in " + subject + " Q" + (i + 1));
                assertNotNull(optC, "Option C cannot be null in " + subject + " Q" + (i + 1));
                assertNotNull(optD, "Option D cannot be null in " + subject + " Q" + (i + 1));

                assertFalse(optA.trim().isEmpty());
                assertFalse(optB.trim().isEmpty());
                assertFalse(optC.trim().isEmpty());
                assertFalse(optD.trim().isEmpty());

                // Ensure all 4 options are distinct
                Set<String> uniqueOptions = new HashSet<>(Arrays.asList(optA.trim(), optB.trim(), optC.trim(), optD.trim()));
                assertEquals(4, uniqueOptions.size(), "All 4 options must be distinct in " + subject + " Q" + (i + 1));

                String correctOption = q.getCorrectOption();
                assertNotNull(correctOption);
                assertTrue(Arrays.asList("A", "B", "C", "D").contains(correctOption.toUpperCase()),
                        "correctOption must be A, B, C, or D in " + subject);

                answerDistribution.put(correctOption.toUpperCase(), answerDistribution.getOrDefault(correctOption.toUpperCase(), 0) + 1);

                String explanation = q.getExplanation();
                assertNotNull(explanation);
                assertFalse(explanation.trim().isEmpty(), "Explanation must be present in " + subject + " Q" + (i + 1));
            }

            // Ensure answers are balanced across options (not all 10 are only A)
            assertTrue(answerDistribution.size() >= 3,
                    "Answer key for " + subject + " should use at least 3 distinct option slots among A, B, C, D");
        }
    }

    // =========================================================================
    // TEST 10: Persistence Prevents Duplicate Phase Regeneration (Priority 5)
    // =========================================================================
    @Test
    @DisplayName("Test 10: Re-opening or enrolling in existing course preserves existing phases")
    void test10_PersistencePreventsDuplicatePhaseGeneration() {
        User student = createStudent("student@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");

        // 1. Initial generation
        Course firstEnrollment = learningPathService.generateSubjectLearningPath(student, "Data Structures & Algorithmic Complexity");
        Long courseId = firstEnrollment.getId();
        int initialPhaseCount = phaseDb.size();
        assertEquals(8, initialPhaseCount);

        // 2. Second access
        Course secondEnrollment = learningPathService.generateSubjectLearningPath(student, "Data Structures & Algorithmic Complexity");
        assertEquals(courseId, secondEnrollment.getId(), "Second enrollment should return existing course instance");
        assertEquals(initialPhaseCount, phaseDb.size(), "No new phases should be created when accessing existing course");
    }

    // =========================================================================
    // TEST 11: Security Audit - Forged Identity, Missing Token & Role Checks
    // =========================================================================
    @Test
    @DisplayName("Test 11: Security audit verifies token enforcement, rejecting forged body parameters and unauthorized roles")
    void test11_CourseApprovalSecurityAudits() {
        createStudent("student@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        createFaculty("hod.cse@siddhartha.ac.in", "Dr. HOD CSE", "CSE");

        // Create a pending course request
        CourseRequest cr = new CourseRequest(
                "student@vrsec.ac.in", "Student VRSEC", "CSE", "2nd Year", "1st Sem",
                "Advanced Operating Systems & Kernel Internals", "Honors preparation"
        );
        cr = courseRequestRepository.save(cr);
        Long reqId = cr.getId();

        Map<String, Object> body = new HashMap<>();
        body.put("requestId", reqId);

        // Case 1: Missing Authorization Header
        ResponseEntity<?> res1 = apiController.approveCourseRequest(null, body);
        assertEquals(HttpStatus.UNAUTHORIZED, res1.getStatusCode(), "Missing token must return 401");

        // Case 2: Invalid Bearer token
        ResponseEntity<?> res2 = apiController.approveCourseRequest("Bearer invalid.tampered.token", body);
        assertEquals(HttpStatus.UNAUTHORIZED, res2.getStatusCode(), "Tampered token must return 401");

        // Case 3: Student token with forged faculty email in body
        String studentToken = "Bearer " + jwtService.generateToken("student@vrsec.ac.in", "STUDENT", 1L);
        Map<String, Object> forgedBody1 = new HashMap<>(body);
        forgedBody1.put("approverEmail", "hod.cse@siddhartha.ac.in"); // Forged in body!
        forgedBody1.put("facultyName", "Dr. HOD CSE");
        ResponseEntity<?> res3 = apiController.approveCourseRequest(studentToken, forgedBody1);
        assertEquals(HttpStatus.FORBIDDEN, res3.getStatusCode(), "Student token with forged body email must return 403");

        // Case 4: Student token with forged role in body
        Map<String, Object> forgedBody2 = new HashMap<>(body);
        forgedBody2.put("role", "FACULTY"); // Forged in body!
        ResponseEntity<?> res4 = apiController.approveCourseRequest(studentToken, forgedBody2);
        assertEquals(HttpStatus.FORBIDDEN, res4.getStatusCode(), "Student token with forged body role must return 403");

        // Case 5: Unauthorized personal email token (not eligible faculty)
        String unauthFacultyToken = "Bearer " + jwtService.generateToken("unauthorized@gmail.com", "FACULTY", 55L);
        ResponseEntity<?> res5 = apiController.approveCourseRequest(unauthFacultyToken, body);
        assertEquals(HttpStatus.FORBIDDEN, res5.getStatusCode(), "Unauthorized email token must return 403");

        // Case 6: Valid authorized faculty token
        String validFacultyToken = "Bearer " + jwtService.generateToken("hod.cse@siddhartha.ac.in", "FACULTY", 99L);
        ResponseEntity<?> res6 = apiController.approveCourseRequest(validFacultyToken, body);
        assertEquals(HttpStatus.OK, res6.getStatusCode(), "Authorized faculty token must return 200 OK");
        assertEquals("APPROVED", cr.getStatus());
    }

    // =========================================================================
    // TEST 12: Course Request Security Audits (Requirement 1 & 3)
    // =========================================================================
    @Test
    @DisplayName("Test 12: Course request security audits - JWT requirement, role validation, and body email spoofing prevention")
    void test12_CourseRequestIdentitySecurityAudits() {
        createStudent("student.a@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        createStudent("student.b@vrsec.ac.in", "ECE", "2nd Year", "1st Sem");
        createFaculty("faculty@siddhartha.ac.in", "Dr. Faculty", "CSE");

        Map<String, String> req = new HashMap<>();
        req.put("title", "Quantum Cryptography & Quantum Key Distribution");
        req.put("reason", "Honors research");

        // 1. Missing JWT on course request -> 401
        ResponseEntity<?> res1 = apiController.requestCourse(null, req);
        assertEquals(HttpStatus.UNAUTHORIZED, res1.getStatusCode(), "Missing token must return 401");

        // 2. Invalid JWT on course request -> 401
        ResponseEntity<?> res2 = apiController.requestCourse("Bearer invalid.tampered.token", req);
        assertEquals(HttpStatus.UNAUTHORIZED, res2.getStatusCode(), "Invalid token must return 401");

        // 3. Faculty attempting student course request -> 403
        String facultyToken = "Bearer " + jwtService.generateToken("faculty@siddhartha.ac.in", "FACULTY", 50L);
        ResponseEntity<?> res3 = apiController.requestCourse(facultyToken, req);
        assertEquals(HttpStatus.FORBIDDEN, res3.getStatusCode(), "Faculty accounts must be rejected with 403");

        // 4. Valid student JWT with body email spoofing: Student A token submits body with Student B email
        String studentAToken = "Bearer " + jwtService.generateToken("student.a@vrsec.ac.in", "STUDENT", 101L);
        Map<String, String> spoofedReq = new HashMap<>(req);
        spoofedReq.put("email", "student.b@vrsec.ac.in"); // Attempt to submit request on behalf of Student B!

        ResponseEntity<?> res4 = apiController.requestCourse(studentAToken, spoofedReq);
        assertEquals(HttpStatus.OK, res4.getStatusCode(), "Valid student token must succeed");

        // Verify that the saved course request record uses Student A's authenticated email, NOT the spoofed body email!
        CourseRequest savedCr = courseRequestDb.values().stream()
                .filter(c -> c.getSuggestedTitle().equals("Quantum Cryptography & Quantum Key Distribution"))
                .findFirst().orElse(null);
        assertNotNull(savedCr);
        assertEquals("student.a@vrsec.ac.in", savedCr.getStudentEmail(), "Saved request MUST use authenticated token identity, completely ignoring body email spoofing!");
    }

    // =========================================================================
    // TEST 13: Dashboard Privacy and Cross-Student Isolation Audits (Requirement 2 & 3)
    // =========================================================================
    @Test
    @DisplayName("Test 13: Dashboard privacy and course requests cross-student isolation audits")
    void test13_DashboardPrivacyAndCrossStudentIsolationAudits() {
        createStudent("student.a@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        createStudent("student.b@vrsec.ac.in", "ECE", "2nd Year", "1st Sem");
        createFaculty("hod.cse@siddhartha.ac.in", "Dr. HOD CSE", "CSE");

        // 1. Missing JWT on dashboard -> 401
        ResponseEntity<?> res1 = apiController.getDashboard(null, "student.a@vrsec.ac.in");
        assertEquals(HttpStatus.UNAUTHORIZED, res1.getStatusCode(), "Missing token on dashboard must return 401");

        // 2. Invalid JWT on dashboard -> 401
        ResponseEntity<?> res2 = apiController.getDashboard("Bearer fake.token", "student.a@vrsec.ac.in");
        assertEquals(HttpStatus.UNAUTHORIZED, res2.getStatusCode(), "Invalid token on dashboard must return 401");

        // 3. Cloud deployment health check exemption -> 200 OK
        ResponseEntity<?> resHealth = apiController.getDashboard(null, "healthcheck");
        assertEquals(HttpStatus.OK, resHealth.getStatusCode(), "Healthcheck must return 200 OK without token");

        // 4. Student A attempting to access Student B's dashboard -> 403 Forbidden!
        String studentAToken = "Bearer " + jwtService.generateToken("student.a@vrsec.ac.in", "STUDENT", 101L);
        ResponseEntity<?> resCrossDashboard = apiController.getDashboard(studentAToken, "student.b@vrsec.ac.in");
        assertEquals(HttpStatus.FORBIDDEN, resCrossDashboard.getStatusCode(), "Student A must NOT access Student B's dashboard (403 Forbidden)");

        // 5. Student A accessing own dashboard -> 200 OK
        ResponseEntity<?> resOwnDashboard = apiController.getDashboard(studentAToken, "student.a@vrsec.ac.in");
        assertEquals(HttpStatus.OK, resOwnDashboard.getStatusCode(), "Student A can access own dashboard (200 OK)");

        // 6. Faculty inspecting Student B's dashboard -> 200 OK
        String facultyToken = "Bearer " + jwtService.generateToken("hod.cse@siddhartha.ac.in", "FACULTY", 99L);
        ResponseEntity<?> resFacultyDashboard = apiController.getDashboard(facultyToken, "student.b@vrsec.ac.in");
        assertEquals(HttpStatus.OK, resFacultyDashboard.getStatusCode(), "Authorized faculty can view student dashboard for academic advising");

        // 7. Student A attempting to retrieve Student B's course requests
        CourseRequest crB = new CourseRequest("student.b@vrsec.ac.in", "Student B", "ECE", "2nd Year", "1st Sem", "Nanoelectronics", "Research");
        courseRequestRepository.save(crB);

        // Student A calls getCourseRequests with Student B's email in query param -> Should return only Student A's requests (empty list)
        ResponseEntity<?> resCrossRequests = apiController.getCourseRequests(studentAToken, "student.b@vrsec.ac.in");
        assertEquals(HttpStatus.OK, resCrossRequests.getStatusCode());
        List<CourseRequest> returnedRequests = (List<CourseRequest>) resCrossRequests.getBody();
        assertNotNull(returnedRequests);
        // Student A should see 0 requests, NOT Student B's request!
        assertTrue(returnedRequests.stream().noneMatch(r -> "student.b@vrsec.ac.in".equalsIgnoreCase(r.getStudentEmail())),
                "Student A must never receive Student B's course requests!");
    }

    // =========================================================================
    // TEST 14: Course Selection Identity & Ownership Enforcement
    // =========================================================================
    @Test
    @DisplayName("Test 14: Course selection identity derived strictly from JWT; cross-student enrollment and faculty enrollment rejected")
    void test14_CourseSelectionIdentityAndOwnership() {
        createStudent("student.a@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        createStudent("student.b@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        createFaculty("faculty@vrsec.ac.in", "Dr. Faculty", "CSE");

        Map<String, Object> req = new HashMap<>();
        req.put("subject", "Data Structures & Algorithmic Complexity");

        // 1. Missing token -> 401 Unauthorized
        ResponseEntity<?> res1 = apiController.selectOrGenerateCourse(null, req);
        assertEquals(HttpStatus.UNAUTHORIZED, res1.getStatusCode());

        // 2. Invalid/tampered token -> 401 Unauthorized
        ResponseEntity<?> res2 = apiController.selectOrGenerateCourse("Bearer invalid.token.signature", req);
        assertEquals(HttpStatus.UNAUTHORIZED, res2.getStatusCode());

        // 3. Faculty token -> 403 Forbidden (Only students can enroll)
        String facultyToken = "Bearer " + jwtService.generateToken("faculty@vrsec.ac.in", "FACULTY", 50L);
        ResponseEntity<?> res3 = apiController.selectOrGenerateCourse(facultyToken, req);
        assertEquals(HttpStatus.FORBIDDEN, res3.getStatusCode());
        assertTrue(res3.getBody().toString().contains("Only students can enroll"));

        // 4. Student A attempts to enroll on behalf of Student B by spoofing body email
        String studentAToken = "Bearer " + jwtService.generateToken("student.a@vrsec.ac.in", "STUDENT", 1L);
        Map<String, Object> spoofedReq = new HashMap<>(req);
        spoofedReq.put("email", "student.b@vrsec.ac.in"); // Spoofed email in body

        ResponseEntity<?> res4 = apiController.selectOrGenerateCourse(studentAToken, spoofedReq);
        assertEquals(HttpStatus.OK, res4.getStatusCode());

        // Verify the created course belongs to Student A (authenticated JWT), NOT Student B!
        assertEquals(1, courseDb.size());
        Course created = courseDb.values().iterator().next();
        assertEquals("student.a@vrsec.ac.in", created.getUserEmail(),
                "Enrolled course MUST belong to Student A derived from JWT, completely ignoring spoofed body email!");

        // Verify Student B has zero enrolled courses
        long studentBCourses = courseDb.values().stream()
                .filter(c -> "student.b@vrsec.ac.in".equalsIgnoreCase(c.getUserEmail()))
                .count();
        assertEquals(0, studentBCourses, "Student B must have 0 courses enrolled!");

        // 5. Enrolling again preserves existing course and does not duplicate
        ResponseEntity<?> res5 = apiController.selectOrGenerateCourse(studentAToken, req);
        assertEquals(HttpStatus.OK, res5.getStatusCode());
        assertEquals(1, courseDb.size(), "Duplicate enrollment must return existing course without duplicating");
    }

    // =========================================================================
    // TEST 15: Profile Update Identity and CGPA Security Audits
    // =========================================================================
    @Test
    @DisplayName("Test 15: Profile updates derive owner strictly from JWT, preventing cross-student CGPA tampering and unauthorized faculty modifications")
    void test15_ProfileUpdateIdentityAndCgpaSecurity() {
        User studentA = createStudent("student.a@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        studentA.setCgpa(8.00);
        User studentB = createStudent("student.b@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        studentB.setCgpa(9.20);
        User faculty = createFaculty("faculty@vrsec.ac.in", "Prof. Tester", "CSE");

        String studentAToken = "Bearer " + jwtService.generateToken("student.a@vrsec.ac.in", "STUDENT", studentA.getId());
        String facultyToken = "Bearer " + jwtService.generateToken("faculty@vrsec.ac.in", "FACULTY", faculty.getId());

        // 1. Missing token -> 401 Unauthorized
        Map<String, Object> req = new HashMap<>();
        req.put("cgpa", "7.50");
        ResponseEntity<?> res1 = apiController.updateProfile(null, req);
        assertEquals(HttpStatus.UNAUTHORIZED, res1.getStatusCode());

        // 2. Tampered token -> 401 Unauthorized
        ResponseEntity<?> res2 = apiController.updateProfile("Bearer fake.token", req);
        assertEquals(HttpStatus.UNAUTHORIZED, res2.getStatusCode());

        // 3. Student A attempts to update Student B's CGPA by supplying Student B's email in body
        Map<String, Object> spoofedUpdate = new HashMap<>();
        spoofedUpdate.put("email", "student.b@vrsec.ac.in"); // Student B targeted in body
        spoofedUpdate.put("cgpa", "6.15");

        ResponseEntity<?> res3 = apiController.updateProfile(studentAToken, spoofedUpdate);
        assertEquals(HttpStatus.OK, res3.getStatusCode());

        // Student B's CGPA must remain untouched (9.20), Student A's CGPA updated to 6.15
        assertEquals(9.20, studentB.getCgpa(), "Student B's CGPA must NOT be altered by Student A's spoofed request!");
        assertEquals(6.15, studentA.getCgpa(), "Student A's own CGPA is updated since identity is derived from JWT!");

        // 4. Faculty token attempting to set student CGPA -> Faculty accounts cannot modify student CGPA (rejected with 400 Bad Request)
        Map<String, Object> facultyReq = new HashMap<>();
        facultyReq.put("email", "student.a@vrsec.ac.in");
        facultyReq.put("cgpa", "9.99");
        ResponseEntity<?> resFaculty = apiController.updateProfile(facultyToken, facultyReq);
        assertEquals(HttpStatus.BAD_REQUEST, resFaculty.getStatusCode());
        assertTrue(resFaculty.getBody().toString().contains("Faculty accounts cannot modify student CGPA"));
        // Student A CGPA is still 6.15
        assertEquals(6.15, studentA.getCgpa(), "Faculty cannot modify student CGPA through profile update endpoint!");
        assertNull(faculty.getCgpa(), "Faculty user has no CGPA");

        // 5. CGPA validation checks
        Map<String, Object> badCgpaReq1 = Map.of("cgpa", "11.50");
        ResponseEntity<?> badRes1 = apiController.updateProfile(studentAToken, badCgpaReq1);
        assertEquals(HttpStatus.BAD_REQUEST, badRes1.getStatusCode());

        Map<String, Object> badCgpaReq2 = Map.of("cgpa", "-2.00");
        ResponseEntity<?> badRes2 = apiController.updateProfile(studentAToken, badCgpaReq2);
        assertEquals(HttpStatus.BAD_REQUEST, badRes2.getStatusCode());

        Map<String, Object> badCgpaReq3 = Map.of("cgpa", "8.999");
        ResponseEntity<?> badRes3 = apiController.updateProfile(studentAToken, badCgpaReq3);
        assertEquals(HttpStatus.BAD_REQUEST, badRes3.getStatusCode());
    }

    // =========================================================================
    // TEST 16: Cross-Student Quiz Submission Security
    // =========================================================================
    @Test
    @DisplayName("Test 16: Cross-student quiz submissions are strictly rejected with HTTP 403 Forbidden")
    void test16_CrossStudentQuizSubmissionSecurity() {
        User studentA = createStudent("student.a@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        User studentB = createStudent("student.b@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");

        Course courseA = learningPathService.generateSubjectLearningPath(studentA, "Data Structures & Algorithmic Complexity");
        Course courseB = learningPathService.generateSubjectLearningPath(studentB, "Data Structures & Algorithmic Complexity");

        String studentAToken = "Bearer " + jwtService.generateToken("student.a@vrsec.ac.in", "STUDENT", studentA.getId());
        String studentBToken = "Bearer " + jwtService.generateToken("student.b@vrsec.ac.in", "STUDENT", studentB.getId());

        // Prepare quiz submission payload for course B
        // Ensure Phase 1 quiz questions are generated and loaded
        learningPathService.getOrCreatePhaseQuiz(courseB.getId(), 1);

        Map<String, Object> subB = new HashMap<>();
        subB.put("courseId", courseB.getId());
        subB.put("phaseNumber", 1);
        Map<String, String> answers = new HashMap<>();
        CoursePhase phaseB = phaseDb.values().stream()
                .filter(p -> p.getCourse().getId().equals(courseB.getId()) && p.getPhaseNumber() == 1)
                .findFirst().orElseThrow();
        List<QuizQuestion> qList = questionRepository.findByPhaseIdOrderByIdAsc(phaseB.getId());
        assertFalse(qList.isEmpty(), "Phase 1 quiz questions must be generated");
        for (QuizQuestion q : qList) {
            answers.put(String.valueOf(q.getId()), q.getCorrectOption());
        }
        subB.put("answers", answers);

        // 1. Missing token -> 401
        ResponseEntity<?> res1 = quizController.submitQuiz(null, subB);
        assertEquals(HttpStatus.UNAUTHORIZED, res1.getStatusCode());

        // 2. Student A attempts to submit quiz answers for Student B's course -> 403 Forbidden!
        ResponseEntity<?> resCross = quizController.submitQuiz(studentAToken, subB);
        assertEquals(HttpStatus.FORBIDDEN, resCross.getStatusCode());
        assertTrue(resCross.getBody().toString().contains("Access denied: You do not own this course"));

        // 3. Student B submits for own course -> 200 OK
        ResponseEntity<?> resOwn = quizController.submitQuiz(studentBToken, subB);
        assertEquals(HttpStatus.OK, resOwn.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) resOwn.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("passed"));
    }

    // =========================================================================
    // TEST 17: Cross-Student Course Progress & Toggle-Delete Security
    // =========================================================================
    @Test
    @DisplayName("Test 17: Cross-student course deletion/modification is strictly rejected with HTTP 403 Forbidden")
    void test17_CrossStudentCourseProgressModification() {
        User studentA = createStudent("student.a@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        User studentB = createStudent("student.b@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");

        Course courseA = learningPathService.generateSubjectLearningPath(studentA, "Data Structures & Algorithmic Complexity");
        Course courseB = learningPathService.generateSubjectLearningPath(studentB, "Data Structures & Algorithmic Complexity");

        String studentAToken = "Bearer " + jwtService.generateToken("student.a@vrsec.ac.in", "STUDENT", studentA.getId());

        // 1. Missing token -> 401 Unauthorized
        Map<String, Object> delPayload = new HashMap<>();
        delPayload.put("courseId", courseB.getId());
        delPayload.put("isDeleted", true);

        ResponseEntity<?> res1 = apiController.toggleCourseDelete(null, delPayload);
        assertEquals(HttpStatus.UNAUTHORIZED, res1.getStatusCode());

        // 2. Student A attempts to delete Student B's course -> 403 Forbidden!
        ResponseEntity<?> resCross = apiController.toggleCourseDelete(studentAToken, delPayload);
        assertEquals(HttpStatus.FORBIDDEN, resCross.getStatusCode());
        assertTrue(resCross.getBody().toString().contains("Access denied: You do not own this course"));

        // Verify Student B's course is NOT deleted
        assertFalse(courseB.isDeleted(), "Student B's course must remain active!");

        // 3. Student A deletes own course -> 200 OK
        Map<String, Object> ownDelPayload = new HashMap<>();
        ownDelPayload.put("courseId", courseA.getId());
        ownDelPayload.put("isDeleted", true);

        ResponseEntity<?> resOwn = apiController.toggleCourseDelete(studentAToken, ownDelPayload);
        assertEquals(HttpStatus.OK, resOwn.getStatusCode());
        assertTrue(courseA.isDeleted(), "Student A's course must be marked deleted");
    }
}
