package com.vrsec.backend;

import com.vrsec.backend.controller.ApiController;
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
    private LearningPathService learningPathService;
    private JwtService jwtService;

    private Map<Long, Course> courseDb;
    private Map<Long, CoursePhase> phaseDb;
    private Map<Long, List<QuizQuestion>> questionDb;
    private Map<String, User> userDb;
    private Map<Long, ApprovedCourse> approvedCourseDb;
    private Map<Long, CourseRequest> courseRequestDb;
    private CourseRequestRepository courseRequestRepository;

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
                    if ("findByUserEmailIgnoreCaseAndDeletedFalse".equals(name)) {
                        String email = (String) args[0];
                        List<Course> list = new ArrayList<>();
                        for (Course c : courseDb.values()) {
                            if (email != null && email.equalsIgnoreCase(c.getUserEmail()) && !c.isDeleted()) {
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
        QuizQuestionRepository questionRepository = (QuizQuestionRepository) Proxy.newProxyInstance(
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

        // Wire LearningPathService dependencies
        ReflectionTestUtils.setField(learningPathService, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(learningPathService, "phaseRepository", phaseRepository);
        ReflectionTestUtils.setField(learningPathService, "questionRepository", questionRepository);
        ReflectionTestUtils.setField(learningPathService, "userRepository", userRepository);
        ReflectionTestUtils.setField(learningPathService, "suggestedCourseRepository", suggestedCourseRepository);
        ReflectionTestUtils.setField(learningPathService, "topicPerformanceRepository", topicPerformanceRepository);
        ReflectionTestUtils.setField(learningPathService, "approvedCourseRepository", approvedCourseRepository);

        // Wire ApiController dependencies
        ReflectionTestUtils.setField(apiController, "userRepository", userRepository);
        ReflectionTestUtils.setField(apiController, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(apiController, "phaseRepository", phaseRepository);
        ReflectionTestUtils.setField(apiController, "approvedCourseRepository", approvedCourseRepository);
        ReflectionTestUtils.setField(apiController, "courseRequestRepository", courseRequestRepository);
        ReflectionTestUtils.setField(apiController, "topicPerformanceRepository", topicPerformanceRepository);
        ReflectionTestUtils.setField(apiController, "learningPathService", learningPathService);

        AuthService authService = new AuthService();
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(apiController, "authService", authService);

        jwtService = new JwtService();
        jwtService.init();
        ReflectionTestUtils.setField(apiController, "jwtService", jwtService);

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

            ResponseEntity<?> res = apiController.selectOrGenerateCourse(req);
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

        Map<String, Object> req = new HashMap<>();
        req.put("email", "student@vrsec.ac.in");
        req.put("subject", "Data Structures & Algorithmic Complexity");

        ResponseEntity<?> res = apiController.selectOrGenerateCourse(req);
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

        ResponseEntity<?> res = apiController.requestCourse(req);
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

        ResponseEntity<?> firstRes = apiController.requestCourse(req);
        assertEquals(HttpStatus.OK, firstRes.getStatusCode());

        // Attempt duplicate request
        ResponseEntity<?> secondRes = apiController.requestCourse(req);
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
        apiController.requestCourse(req);

        CourseRequest pendingReq = courseRequestDb.values().iterator().next();
        Long requestId = pendingReq.getId();

        // 2a. SECURITY CHECK: Student attempts to approve course request -> MUST BE REJECTED WITH 403 FORBIDDEN
        String studentToken = "Bearer " + jwtService.generateToken("student@vrsec.ac.in", "STUDENT", 1L);
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

        ResponseEntity<?> enrollRes = apiController.selectOrGenerateCourse(enrollPayload);
        assertEquals(HttpStatus.OK, enrollRes.getStatusCode());
        assertEquals(1, courseDb.size());

        // 4. Ineligible student enrollment check: A Mechanical student attempting to enroll in an ECE Core course
        User mechStudent = createStudent("mech.student@vrsec.ac.in", "MECH", "2nd Year", "1st Sem");
        Map<String, Object> ineligiblePayload = new HashMap<>();
        ineligiblePayload.put("email", "mech.student@vrsec.ac.in");
        ineligiblePayload.put("subject", "Electronic Devices & Circuit Theory"); // ECE CORE course

        ResponseEntity<?> ineligibleRes = apiController.selectOrGenerateCourse(ineligiblePayload);
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
}
