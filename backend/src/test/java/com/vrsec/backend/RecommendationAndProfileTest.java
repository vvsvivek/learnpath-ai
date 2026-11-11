package com.vrsec.backend;

import com.vrsec.backend.controller.ApiController;
import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import com.vrsec.backend.service.AuthService;
import com.vrsec.backend.service.LearningPathService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class RecommendationAndProfileTest {

    private ApiController apiController;
    private LearningPathService learningPathService;
    private AuthService authService;

    private Map<String, User> userDb;
    private Map<Long, Course> courseDb;
    private List<SuggestedCourse> suggestedCoursesDb;
    private List<TopicPerformance> topicPerformanceDb;

    @BeforeEach
    void setUp() {
        apiController = new ApiController();
        learningPathService = new LearningPathService();
        authService = new AuthService();

        userDb = new ConcurrentHashMap<>();
        courseDb = new ConcurrentHashMap<>();
        suggestedCoursesDb = new ArrayList<>();
        topicPerformanceDb = new ArrayList<>();

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
                    if ("save".equals(name)) {
                        User u = (User) args[0];
                        if (u.getId() == null) u.setId((long) (userDb.size() + 1));
                        userDb.put(u.getEmail().toLowerCase(), u);
                        return u;
                    }
                    return null;
                }
        );

        // 2. Mock CourseRepository
        CourseRepository courseRepository = (CourseRepository) Proxy.newProxyInstance(
                CourseRepository.class.getClassLoader(),
                new Class<?>[]{CourseRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findByUserEmailIgnoreCaseAndDeletedFalse".equals(name) || "findByUserEmailIgnoreCase".equals(name)) {
                        String email = (String) args[0];
                        return courseDb.values().stream()
                                .filter(c -> c.getUserEmail() != null && c.getUserEmail().equalsIgnoreCase(email) && !c.isDeleted())
                                .collect(Collectors.toList());
                    }
                    if ("save".equals(name)) {
                        Course c = (Course) args[0];
                        if (c.getId() == null) c.setId((long) (courseDb.size() + 1));
                        courseDb.put(c.getId(), c);
                        return c;
                    }
                    return Collections.emptyList();
                }
        );

        // 3. Mock SuggestedCourseRepository
        SuggestedCourseRepository suggestedCourseRepository = (SuggestedCourseRepository) Proxy.newProxyInstance(
                SuggestedCourseRepository.class.getClassLoader(),
                new Class<?>[]{SuggestedCourseRepository.class},
                (proxy, method, args) -> {
                    if ("findAll".equals(method.getName())) {
                        return new ArrayList<>(suggestedCoursesDb);
                    }
                    return Collections.emptyList();
                }
        );

        // 4. Mock TopicPerformanceRepository
        TopicPerformanceRepository topicPerformanceRepository = (TopicPerformanceRepository) Proxy.newProxyInstance(
                TopicPerformanceRepository.class.getClassLoader(),
                new Class<?>[]{TopicPerformanceRepository.class},
                (proxy, method, args) -> {
                    if ("findByUserIdAndStatus".equals(method.getName())) {
                        Long uid = (Long) args[0];
                        String st = (String) args[1];
                        return topicPerformanceDb.stream()
                                .filter(tp -> tp.getUser() != null && uid.equals(tp.getUser().getId()) && st.equalsIgnoreCase(tp.getStatus()))
                                .collect(Collectors.toList());
                    }
                    return Collections.emptyList();
                }
        );

        ReflectionTestUtils.setField(learningPathService, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(learningPathService, "suggestedCourseRepository", suggestedCourseRepository);
        ReflectionTestUtils.setField(learningPathService, "topicPerformanceRepository", topicPerformanceRepository);

        ReflectionTestUtils.setField(apiController, "userRepository", userRepository);
        ReflectionTestUtils.setField(apiController, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(apiController, "learningPathService", learningPathService);
        ReflectionTestUtils.setField(apiController, "authService", authService);
    }

    // =========================================================================
    // SCENARIO 1: New CSE student completes profile with CGPA
    // =========================================================================
    @Test
    void testScenario1_NewCseStudentCompletesProfileWithCgpa() {
        String email = "fresh.cse@gmail.com";
        User user = new User();
        user.setEmail(email);
        user.setUsername("Fresh CSE");
        user.setRole("STUDENT");
        user.setBranch(null);
        user.setAcademicYear(null);
        user.setSemester(null);
        user.setCgpa(null);
        userDb.put(email, user);

        Map<String, Object> req = new HashMap<>();
        req.put("email", email);
        req.put("branch", "CSE");
        req.put("academicYear", "2nd Year");
        req.put("semester", "1st Sem");
        req.put("cgpa", "8.75");

        ResponseEntity<?> response = apiController.updateProfile(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        User updated = userDb.get(email);
        assertEquals("CSE", updated.getBranch());
        assertEquals("2nd Year", updated.getAcademicYear());
        assertEquals("1st Sem", updated.getSemester());
        assertEquals(8.75, updated.getCgpa());

        // Verify recommendations
        List<Map<String, Object>> recs = learningPathService.getPersonalizedRecommendations(updated);
        assertFalse(recs.isEmpty());

        List<String> titles = recs.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());
        assertTrue(titles.contains("Data Structures & Algorithmic Complexity"), "CSE 2nd Year 1st Sem must recommend Data Structures");
        assertTrue(titles.contains("Object-Oriented Programming with Java & C++"), "CSE 2nd Year 1st Sem must recommend OOP");
        assertTrue(titles.contains("Competitive Programming & Advanced Algorithmic Mastery"), "CGPA 8.75 must trigger Honors track");

        // Verify reason contains curriculum match explanation
        Map<String, Object> dsaRec = recs.stream().filter(r -> r.get("title").equals("Data Structures & Algorithmic Complexity")).findFirst().orElseThrow();
        String reason = (String) dsaRec.get("reason");
        assertTrue(reason.contains("CSE") && reason.contains("1st Sem"), "Reason must clearly cite branch and semester");
    }

    // =========================================================================
    // SCENARIO 2: ECE student receives ECE-relevant recommendations
    // =========================================================================
    @Test
    void testScenario2_EceStudentReceivesEceRelevantRecommendations() {
        User eceStudent = new User();
        eceStudent.setEmail("ece.student@vrsec.ac.in");
        eceStudent.setBranch("ECE");
        eceStudent.setAcademicYear("2nd Year");
        eceStudent.setSemester("1st Sem");
        eceStudent.setCgpa(7.8);
        eceStudent.setRole("STUDENT");

        List<Map<String, Object>> recs = learningPathService.getPersonalizedRecommendations(eceStudent);
        assertFalse(recs.isEmpty());

        List<String> titles = recs.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());
        assertTrue(titles.contains("Electronic Devices & Circuit Theory"), "ECE 2nd Year 1st Sem must recommend Electronic Devices");
        assertTrue(titles.contains("Digital Logic Design & Verilog Modeling"), "ECE 2nd Year 1st Sem must recommend Digital Logic");
        assertTrue(titles.contains("Signals & Systems Analysis"), "ECE 2nd Year 1st Sem must recommend Signals & Systems");

        // Zero cross-branch contamination: ECE should never receive Mechanical or Civil or CSE specific courses
        assertFalse(titles.contains("Mechanics of Solids & Strength of Materials"), "ECE must not receive Mechanical courses");
        assertFalse(titles.contains("Structural Analysis & Energy Methods"), "ECE must not receive Civil courses");
        assertFalse(titles.contains("Database Management Systems & SQL Mastery"), "ECE must not receive CSE core courses");
    }

    // =========================================================================
    // SCENARIO 3: EEE student receives EEE-relevant recommendations
    // =========================================================================
    @Test
    void testScenario3_EeeStudentReceivesEeeRelevantRecommendations() {
        User eeeStudent = new User();
        eeeStudent.setEmail("eee.student@vrsec.ac.in");
        eeeStudent.setBranch("EEE");
        eeeStudent.setAcademicYear("2nd Year");
        eeeStudent.setSemester("1st Sem");
        eeeStudent.setCgpa(8.0);
        eeeStudent.setRole("STUDENT");

        List<Map<String, Object>> recs = learningPathService.getPersonalizedRecommendations(eeeStudent);
        assertFalse(recs.isEmpty());

        List<String> titles = recs.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());
        assertTrue(titles.contains("Electric Circuit Theory & Network Analysis"), "EEE must recommend Electric Circuit Theory");
        assertTrue(titles.contains("DC Machines & Transformers"), "EEE must recommend DC Machines & Transformers");
        assertTrue(titles.contains("Electromagnetic Field Theory"), "EEE must recommend Electromagnetic Field Theory");

        // Zero cross-branch contamination
        assertFalse(titles.contains("Mechanics of Solids & Strength of Materials"));
        assertFalse(titles.contains("Electronic Devices & Circuit Theory"));
    }

    // =========================================================================
    // SCENARIO 4: Mechanical student receives Mechanical-relevant recommendations
    // =========================================================================
    @Test
    void testScenario4_MechanicalStudentReceivesMechanicalRelevantRecommendations() {
        User mechStudent = new User();
        mechStudent.setEmail("24eu04191@vrsec.ac.in");
        mechStudent.setBranch("MECH");
        mechStudent.setAcademicYear("2nd Year");
        mechStudent.setSemester("1st Sem");
        mechStudent.setCgpa(8.2);
        mechStudent.setRole("STUDENT");

        List<Map<String, Object>> recs = learningPathService.getPersonalizedRecommendations(mechStudent);
        assertFalse(recs.isEmpty());

        List<String> titles = recs.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());
        assertTrue(titles.contains("Mechanics of Solids & Strength of Materials"), "MECH must recommend Mechanics of Solids");
        assertTrue(titles.contains("Thermodynamics & Heat Transfer Fundamentals"), "MECH must recommend Thermodynamics");
        assertTrue(titles.contains("Material Science & Metallurgy for Engineers"), "MECH must recommend Material Science");

        // Zero cross-branch contamination
        assertFalse(titles.contains("Data Structures & Algorithmic Complexity"));
        assertFalse(titles.contains("Electronic Devices & Circuit Theory"));
        assertFalse(titles.contains("Electric Circuit Theory & Network Analysis"));
    }

    // =========================================================================
    // SCENARIO 5: Students in different academic years receive different recommendations
    // =========================================================================
    @Test
    void testScenario5_DifferentYearsReceiveDifferentRecommendations() {
        User cse2ndYear = new User();
        cse2ndYear.setBranch("CSE");
        cse2ndYear.setAcademicYear("2nd Year");
        cse2ndYear.setSemester("1st Sem");

        User cse3rdYear = new User();
        cse3rdYear.setBranch("CSE");
        cse3rdYear.setAcademicYear("3rd Year");
        cse3rdYear.setSemester("1st Sem");

        List<Map<String, Object>> recs2 = learningPathService.getPersonalizedRecommendations(cse2ndYear);
        List<Map<String, Object>> recs3 = learningPathService.getPersonalizedRecommendations(cse3rdYear);

        List<String> titles2 = recs2.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());
        List<String> titles3 = recs3.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());

        assertTrue(titles2.contains("Data Structures & Algorithmic Complexity"));
        assertFalse(titles2.contains("Operating Systems & Kernel Process Modeling"), "2nd Year should not get 3rd Year OS as Tier 1");

        assertTrue(titles3.contains("Operating Systems & Kernel Process Modeling"));
        assertTrue(titles3.contains("Computer Networks & Protocol Architectures"));
        assertFalse(titles3.contains("Data Structures & Algorithmic Complexity"), "3rd Year should not get 2nd Year Data Structures as Tier 1");
    }

    // =========================================================================
    // SCENARIO 6: Students in different semesters receive different recommendations
    // =========================================================================
    @Test
    void testScenario6_DifferentSemestersReceiveDifferentRecommendations() {
        User cseSem1 = new User();
        cseSem1.setBranch("CSE");
        cseSem1.setAcademicYear("2nd Year");
        cseSem1.setSemester("1st Sem");

        User cseSem2 = new User();
        cseSem2.setBranch("CSE");
        cseSem2.setAcademicYear("2nd Year");
        cseSem2.setSemester("2nd Sem");

        List<Map<String, Object>> recsSem1 = learningPathService.getPersonalizedRecommendations(cseSem1);
        List<Map<String, Object>> recsSem2 = learningPathService.getPersonalizedRecommendations(cseSem2);

        List<String> titles1 = recsSem1.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());
        List<String> titles2 = recsSem2.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());

        assertTrue(titles1.contains("Data Structures & Algorithmic Complexity"));
        assertTrue(titles2.contains("Database Management Systems & SQL Mastery"));
        assertTrue(titles2.contains("Design & Analysis of Algorithms"));
    }

    // =========================================================================
    // SCENARIO 7: Student updates branch, year or semester and recommendations refresh
    // =========================================================================
    @Test
    void testScenario7_StudentUpdatesProfileAndRecommendationsRefresh() {
        String email = "transfer.student@vrsec.ac.in";
        User user = new User();
        user.setEmail(email);
        user.setRole("STUDENT");
        user.setBranch("MECH");
        user.setAcademicYear("2nd Year");
        user.setSemester("1st Sem");
        user.setCgpa(8.0);
        userDb.put(email, user);

        // Before update: Mechanical recommendations
        List<Map<String, Object>> initialRecs = learningPathService.getPersonalizedRecommendations(user);
        List<String> initialTitles = initialRecs.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());
        assertTrue(initialTitles.contains("Mechanics of Solids & Strength of Materials"));

        // Student changes branch to CSE
        Map<String, Object> updateReq = new HashMap<>();
        updateReq.put("email", email);
        updateReq.put("branch", "CSE");
        updateReq.put("academicYear", "2nd Year");
        updateReq.put("semester", "1st Sem");
        updateReq.put("cgpa", "8.00");

        ResponseEntity<?> response = apiController.updateProfile(updateReq);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        User refreshedUser = userDb.get(email);
        assertEquals("CSE", refreshedUser.getBranch());

        // After update: CSE recommendations
        List<Map<String, Object>> refreshedRecs = learningPathService.getPersonalizedRecommendations(refreshedUser);
        List<String> refreshedTitles = refreshedRecs.stream().map(r -> (String) r.get("title")).collect(Collectors.toList());
        assertTrue(refreshedTitles.contains("Data Structures & Algorithmic Complexity"));
        assertFalse(refreshedTitles.contains("Mechanics of Solids & Strength of Materials"));
    }

    // =========================================================================
    // SCENARIO 8: CGPA validation rejects values below 0 or above 10
    // =========================================================================
    @Test
    void testScenario8_CgpaValidationRejectsInvalidValues() {
        String email = "validation.test@gmail.com";
        User user = new User();
        user.setEmail(email);
        user.setRole("STUDENT");
        userDb.put(email, user);

        // A. Negative CGPA
        Map<String, Object> req1 = Map.of("email", email, "cgpa", "-0.50");
        ResponseEntity<?> res1 = apiController.updateProfile(req1);
        assertEquals(HttpStatus.BAD_REQUEST, res1.getStatusCode());
        assertTrue(res1.getBody().toString().contains("between 0.00 and 10.00"));

        // B. CGPA above 10.00
        Map<String, Object> req2 = Map.of("email", email, "cgpa", "10.05");
        ResponseEntity<?> res2 = apiController.updateProfile(req2);
        assertEquals(HttpStatus.BAD_REQUEST, res2.getStatusCode());
        assertTrue(res2.getBody().toString().contains("between 0.00 and 10.00"));

        // C. Non-numeric CGPA
        Map<String, Object> req3 = Map.of("email", email, "cgpa", "nine_point_five");
        ResponseEntity<?> res3 = apiController.updateProfile(req3);
        assertEquals(HttpStatus.BAD_REQUEST, res3.getStatusCode());
        assertTrue(res3.getBody().toString().contains("valid numerical value"));

        // D. More than 2 decimal places
        Map<String, Object> req4 = Map.of("email", email, "cgpa", "8.756");
        ResponseEntity<?> res4 = apiController.updateProfile(req4);
        assertEquals(HttpStatus.BAD_REQUEST, res4.getStatusCode());
        assertTrue(res4.getBody().toString().contains("2 decimal places only"));

        // E. Valid boundaries: 0.00 and 10.00
        Map<String, Object> req5 = Map.of("email", email, "cgpa", "0.00");
        ResponseEntity<?> res5 = apiController.updateProfile(req5);
        assertEquals(HttpStatus.OK, res5.getStatusCode());
        assertEquals(0.00, userDb.get(email).getCgpa());

        Map<String, Object> req6 = Map.of("email", email, "cgpa", "10.00");
        ResponseEntity<?> res6 = apiController.updateProfile(req6);
        assertEquals(HttpStatus.OK, res6.getStatusCode());
        assertEquals(10.00, userDb.get(email).getCgpa());
    }

    // =========================================================================
    // SCENARIO 9: Existing student with incomplete profile can complete only missing fields
    // =========================================================================
    @Test
    void testScenario9_ExistingStudentIncompleteProfileCompletesOnlyMissingFields() {
        String email = "24eu04191@vrsec.ac.in";
        User existingUser = new User();
        existingUser.setId(45L);
        existingUser.setEmail(email);
        existingUser.setUsername("Mech Student");
        existingUser.setRole("STUDENT");
        existingUser.setBranch("MECH");
        existingUser.setAcademicYear("2nd Year");
        existingUser.setSemester("1st Sem");
        existingUser.setCgpa(null); // Missing CGPA
        userDb.put(email, existingUser);

        // Student submits only their missing CGPA
        Map<String, Object> updateOnlyCgpa = new HashMap<>();
        updateOnlyCgpa.put("email", email);
        updateOnlyCgpa.put("cgpa", "8.45");

        ResponseEntity<?> response = apiController.updateProfile(updateOnlyCgpa);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        User updated = userDb.get(email);
        assertEquals("MECH", updated.getBranch(), "Branch must remain intact");
        assertEquals("2nd Year", updated.getAcademicYear(), "Year must remain intact");
        assertEquals("1st Sem", updated.getSemester(), "Semester must remain intact");
        assertEquals(8.45, updated.getCgpa(), "CGPA must be saved");
    }

    // =========================================================================
    // SCENARIO 10: Faculty login and profile remain unaffected
    // =========================================================================
    @Test
    void testScenario10_FacultyLoginAndProfileRemainUnaffected() {
        User facultyUser = new User();
        facultyUser.setEmail("hod.cse@siddhartha.ac.in");
        facultyUser.setRole("FACULTY");
        facultyUser.setBranch("CSE");
        facultyUser.setAcademicYear("Faculty");
        facultyUser.setSemester("N/A");
        facultyUser.setCgpa(null); // Faculty doesn't have student CGPA

        assertTrue(facultyUser.isFaculty());
        assertFalse(facultyUser.isEligibleStudent());

        Map<String, Object> sanitized = authService.sanitizeUser(facultyUser);
        assertEquals("FACULTY", sanitized.get("role"));
        assertNull(sanitized.get("cgpa"));
    }
}
