package com.vrsec.backend;

import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import com.vrsec.backend.service.LearningPathService;
import com.vrsec.backend.service.SubjectCurriculumRegistry;
import com.vrsec.backend.service.SubjectQuestionBank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class SubjectLearningPathAndQuizTest {

    private LearningPathService learningPathService;
    private Map<Long, Course> courseDb;
    private Map<Long, CoursePhase> phaseDb;
    private Map<Long, List<QuizQuestion>> questionDb;
    private Map<String, User> userDb;

    @BeforeEach
    void setUp() {
        learningPathService = new LearningPathService();
        courseDb = new ConcurrentHashMap<>();
        phaseDb = new ConcurrentHashMap<>();
        questionDb = new ConcurrentHashMap<>();
        userDb = new ConcurrentHashMap<>();

        // Mock CourseRepository
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

        // Mock CoursePhaseRepository
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

        // Mock QuizQuestionRepository
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
                    if ("deleteAll".equals(name)) {
                        List<QuizQuestion> toDelete = (List<QuizQuestion>) args[0];
                        for (List<QuizQuestion> list : questionDb.values()) {
                            list.removeAll(toDelete);
                        }
                        return null;
                    }
                    if ("flush".equals(name)) {
                        return null;
                    }
                    return null;
                }
        );

        // Mock UserRepository
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

        // Mock SuggestedCourseRepository
        SuggestedCourseRepository suggestedCourseRepository = (SuggestedCourseRepository) Proxy.newProxyInstance(
                SuggestedCourseRepository.class.getClassLoader(),
                new Class<?>[]{SuggestedCourseRepository.class},
                (proxy, method, args) -> Collections.emptyList()
        );

        // Mock TopicPerformanceRepository
        TopicPerformanceRepository topicPerformanceRepository = (TopicPerformanceRepository) Proxy.newProxyInstance(
                TopicPerformanceRepository.class.getClassLoader(),
                new Class<?>[]{TopicPerformanceRepository.class},
                (proxy, method, args) -> Collections.emptyList()
        );

        ReflectionTestUtils.setField(learningPathService, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(learningPathService, "phaseRepository", phaseRepository);
        ReflectionTestUtils.setField(learningPathService, "questionRepository", questionRepository);
        ReflectionTestUtils.setField(learningPathService, "userRepository", userRepository);
        ReflectionTestUtils.setField(learningPathService, "suggestedCourseRepository", suggestedCourseRepository);
        ReflectionTestUtils.setField(learningPathService, "topicPerformanceRepository", topicPerformanceRepository);
    }

    private User createStudent(String email, String branch, String year, String sem) {
        User u = new User();
        u.setId(1L);
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
    // SCENARIO 1: Data Structures learning path phases
    // =========================================================================
    @Test
    @DisplayName("Scenario 1: Data Structures learning path contains authentic 8 DSA phases")
    void test1_DataStructuresLearningPathPhases() {
        User student = createStudent("student@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        List<Map<String, Object>> phases = SubjectCurriculumRegistry.getCurriculumForSubject("Data Structures & Algorithms", student);

        assertNotNull(phases);
        assertEquals(8, phases.size(), "Data Structures must have exactly 8 phases");

        String phase1Title = (String) phases.get(0).get("title");
        String phase3Title = (String) phases.get(2).get("title");
        String phase5Title = (String) phases.get(4).get("title");
        String phase7Title = (String) phases.get(6).get("title");

        assertTrue(phase1Title.toLowerCase().contains("complexity") || phase1Title.toLowerCase().contains("big-o"),
                "Phase 1 should be Big-O / Complexity");
        assertTrue(phase3Title.toLowerCase().contains("linked list"),
                "Phase 3 should be Linked Lists");
        assertTrue(phase5Title.toLowerCase().contains("tree"),
                "Phase 5 should be Trees / BST / AVL");
        assertTrue(phase7Title.toLowerCase().contains("graph"),
                "Phase 7 should be Graphs");

        // Verify none of the phases are generic templates
        for (Map<String, Object> p : phases) {
            String title = (String) p.get("title");
            assertFalse(title.contains("Fundamental Building Blocks & Syntax"), "Generic phase detected in DSA path: " + title);
            assertFalse(title.contains("Capstone Implementation & Performance Engineering"), "Generic phase detected in DSA path: " + title);
        }
    }

    // =========================================================================
    // SCENARIO 2: Engineering Mathematics learning path phases
    // =========================================================================
    @Test
    @DisplayName("Scenario 2: Engineering Mathematics learning path contains authentic 6 Mathematics phases")
    void test2_EngineeringMathematicsLearningPathPhases() {
        User student = createStudent("student@vrsec.ac.in", "CSE", "1st Year", "1st Sem");
        List<Map<String, Object>> phases = SubjectCurriculumRegistry.getCurriculumForSubject("Engineering Mathematics & Calculus", student);

        assertNotNull(phases);
        assertEquals(6, phases.size(), "Engineering Mathematics must have 6 phases");

        String p1 = (String) phases.get(0).get("title");
        String p2 = (String) phases.get(1).get("title");
        String p4 = (String) phases.get(3).get("title");
        String p6 = (String) phases.get(5).get("title");

        assertTrue(p1.contains("Rolle's") || p1.contains("Calculus") || p1.contains("Differential"), "Phase 1 should cover Differential Calculus");
        assertTrue(p2.contains("Partial Derivatives") || p2.contains("Euler") || p2.contains("Jacobian"), "Phase 2 should cover Partial Derivatives & Jacobians");
        assertTrue(p4.contains("Vector Calculus") || p4.contains("Gradient") || p4.contains("Divergence"), "Phase 4 should cover Vector Calculus");
        assertTrue(p6.contains("Laplace"), "Phase 6 should cover Laplace Transforms");

        for (Map<String, Object> p : phases) {
            String title = (String) p.get("title");
            assertFalse(title.contains("Fundamental Building Blocks & Syntax"), "Generic phase in Math: " + title);
        }
    }

    // =========================================================================
    // SCENARIO 3: Digital Electronics learning path phases
    // =========================================================================
    @Test
    @DisplayName("Scenario 3: Digital Electronics learning path contains authentic 6 Electronics phases")
    void test3_DigitalElectronicsLearningPathPhases() {
        User student = createStudent("student@vrsec.ac.in", "ECE", "2nd Year", "1st Sem");
        List<Map<String, Object>> phases = SubjectCurriculumRegistry.getCurriculumForSubject("Digital Logic Design & Verilog Modeling", student);

        assertNotNull(phases);
        assertEquals(6, phases.size());

        assertTrue(((String) phases.get(0).get("title")).contains("Number Systems") || ((String) phases.get(0).get("title")).contains("Complement"));
        assertTrue(((String) phases.get(1).get("title")).contains("Boolean") || ((String) phases.get(1).get("title")).contains("K-Map"));
        assertTrue(((String) phases.get(3).get("title")).contains("Combinational") || ((String) phases.get(3).get("title")).contains("Adders"));
        assertTrue(((String) phases.get(4).get("title")).contains("Sequential") || ((String) phases.get(4).get("title")).contains("Flip-Flops"));
        assertTrue(((String) phases.get(5).get("title")).contains("Counters") || ((String) phases.get(5).get("title")).contains("Shift Registers"));
    }

    // =========================================================================
    // SCENARIO 4: Engineering Physics learning path phases
    // =========================================================================
    @Test
    @DisplayName("Scenario 4: Engineering Physics learning path contains authentic 6 Physics phases")
    void test4_EngineeringPhysicsLearningPathPhases() {
        User student = createStudent("student@vrsec.ac.in", "MECH", "1st Year", "1st Sem");
        List<Map<String, Object>> phases = SubjectCurriculumRegistry.getCurriculumForSubject("Engineering Physics & Semiconductor Basics", student);

        assertNotNull(phases);
        assertEquals(6, phases.size());

        assertTrue(((String) phases.get(0).get("title")).contains("Interference") || ((String) phases.get(0).get("title")).contains("Wave Optics"));
        assertTrue(((String) phases.get(1).get("title")).contains("Diffraction"));
        assertTrue(((String) phases.get(2).get("title")).contains("Polarization"));
        assertTrue(((String) phases.get(3).get("title")).contains("Lasers"));
        assertTrue(((String) phases.get(4).get("title")).contains("Fiber Optics"));
        assertTrue(((String) phases.get(5).get("title")).contains("Semiconductor"));
    }

    // =========================================================================
    // SCENARIO 5: Non-identical learning paths across subjects
    // =========================================================================
    @Test
    @DisplayName("Scenario 5: Non-identical learning paths between Data Structures, Mathematics, Electronics, and Physics")
    void test5_NonIdenticalLearningPaths() {
        User student = createStudent("student@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");

        List<Map<String, Object>> dsaPhases = SubjectCurriculumRegistry.getCurriculumForSubject("Data Structures & Algorithms", student);
        List<Map<String, Object>> mathPhases = SubjectCurriculumRegistry.getCurriculumForSubject("Engineering Mathematics & Calculus", student);
        List<Map<String, Object>> ecePhases = SubjectCurriculumRegistry.getCurriculumForSubject("Digital Logic Design & Verilog", student);
        List<Map<String, Object>> phyPhases = SubjectCurriculumRegistry.getCurriculumForSubject("Engineering Physics & Semiconductor", student);

        Set<String> dsaTitles = new HashSet<>();
        dsaPhases.forEach(p -> dsaTitles.add((String) p.get("title")));

        Set<String> mathTitles = new HashSet<>();
        mathPhases.forEach(p -> mathTitles.add((String) p.get("title")));

        Set<String> eceTitles = new HashSet<>();
        ecePhases.forEach(p -> eceTitles.add((String) p.get("title")));

        Set<String> phyTitles = new HashSet<>();
        phyPhases.forEach(p -> phyTitles.add((String) p.get("title")));

        // Zero title overlap between distinct courses!
        Set<String> overlap1 = new HashSet<>(dsaTitles);
        overlap1.retainAll(mathTitles);
        assertTrue(overlap1.isEmpty(), "DSA and Math should have zero overlapping phase titles");

        Set<String> overlap2 = new HashSet<>(mathTitles);
        overlap2.retainAll(eceTitles);
        assertTrue(overlap2.isEmpty(), "Math and ECE should have zero overlapping phase titles");

        Set<String> overlap3 = new HashSet<>(eceTitles);
        overlap3.retainAll(phyTitles);
        assertTrue(overlap3.isEmpty(), "ECE and Physics should have zero overlapping phase titles");
    }

    // =========================================================================
    // SCENARIO 6: Quiz relevance to course
    // =========================================================================
    @Test
    @DisplayName("Scenario 6: Data Structures quiz contains DSA questions and no mathematics/physics questions")
    void test6_QuizRelevanceToCourse() {
        Course dsaCourse = new Course();
        dsaCourse.setId(10L);
        dsaCourse.setTitle("Data Structures & Algorithmic Complexity");

        CoursePhase phase = new CoursePhase();
        phase.setId(101L);
        phase.setCourse(dsaCourse);
        phase.setPhaseNumber(1);
        phase.setTitle("Phase 1: Algorithmic Complexity, Big-O Notation & Asymptotic Analysis");

        List<QuizQuestion> questions = SubjectQuestionBank.getQuestionsForPhase(phase, null);
        assertEquals(10, questions.size());

        for (QuizQuestion q : questions) {
            String text = q.getQuestionText().toLowerCase();
            String tag = q.getConceptTag().toLowerCase();
            // Should contain DSA concepts in text or tag
            boolean hasDsaConcept = text.contains("time complexity") || text.contains("recurrence") || text.contains("asymptotic")
                    || text.contains("amortized") || text.contains("master theorem") || text.contains("space complexity")
                    || text.contains("bound") || text.contains("heapify") || text.contains("growth") || text.contains("search")
                    || text.contains("little-o") || text.contains("big-theta") || text.contains("big-o") || text.contains("algorithm")
                    || tag.contains("complexity") || tag.contains("theorem") || tag.contains("bound");
            assertTrue(hasDsaConcept, "DSA question should test DSA concepts: " + text);
            // Must NOT contain unrelated physics/math concepts
            assertFalse(text.contains("rolle's theorem"), "Math question in DSA quiz: " + text);
            assertFalse(text.contains("newton's rings"), "Physics question in DSA quiz: " + text);
            assertFalse(text.contains("kirchhoff"), "Electrical question in DSA quiz: " + text);
        }
    }

    // =========================================================================
    // SCENARIO 7: Questions match specific phase
    // =========================================================================
    @Test
    @DisplayName("Scenario 7: Phase 1 questions test Phase 1 topics, Phase 2 questions test Phase 2 topics")
    void test7_QuestionsMatchPhase() {
        Course course = new Course();
        course.setId(20L);
        course.setTitle("Data Structures & Algorithmic Complexity");

        CoursePhase p1 = new CoursePhase();
        p1.setId(201L);
        p1.setCourse(course);
        p1.setPhaseNumber(1);
        p1.setTitle("Phase 1: Algorithmic Complexity & Big-O Notation");

        CoursePhase p2 = new CoursePhase();
        p2.setId(202L);
        p2.setCourse(course);
        p2.setPhaseNumber(2);
        p2.setTitle("Phase 2: Contiguous Arrays, Strings & Two-Pointer Manipulation");

        List<QuizQuestion> q1List = SubjectQuestionBank.getQuestionsForPhase(p1, null);
        List<QuizQuestion> q2List = SubjectQuestionBank.getQuestionsForPhase(p2, null);

        // Phase 1 questions should test Big-O, Master theorem, bounds
        assertTrue(q1List.get(0).getConceptTag().contains("Master Theorem") || q1List.get(0).getQuestionText().contains("Master Theorem"));
        // Phase 2 questions should test Arrays, Kadane, Row-Major, Two pointers
        assertTrue(q2List.get(0).getConceptTag().contains("Array Memory Layout") || q2List.get(0).getQuestionText().contains("2D array"));
        assertTrue(q2List.get(1).getConceptTag().contains("Kadane") || q2List.get(1).getQuestionText().contains("Kadane"));
    }

    // =========================================================================
    // SCENARIO 8: Options appropriateness (4 distinct options per question)
    // =========================================================================
    @Test
    @DisplayName("Scenario 8: Each question has 4 distinct options with zero duplicate options")
    void test8_OptionsAppropriateness() {
        Course course = new Course();
        course.setId(30L);
        course.setTitle("Engineering Mathematics & Calculus");

        CoursePhase phase = new CoursePhase();
        phase.setId(301L);
        phase.setCourse(course);
        phase.setPhaseNumber(1);
        phase.setTitle("Phase 1: Differential Calculus, Rolle's & LMVT");

        List<QuizQuestion> questions = SubjectQuestionBank.getQuestionsForPhase(phase, null);
        assertEquals(10, questions.size());

        for (QuizQuestion q : questions) {
            assertNotNull(q.getOptionA());
            assertNotNull(q.getOptionB());
            assertNotNull(q.getOptionC());
            assertNotNull(q.getOptionD());

            Set<String> distinct = new HashSet<>(Arrays.asList(
                    q.getOptionA().trim(), q.getOptionB().trim(), q.getOptionC().trim(), q.getOptionD().trim()));
            assertEquals(4, distinct.size(), "Question options must be completely distinct: " + q.getQuestionText());
        }
    }

    // =========================================================================
    // SCENARIO 9: Correct answers validated and distributed (not all A!)
    // =========================================================================
    @Test
    @DisplayName("Scenario 9: Correct answers are distributed among A, B, C, and D (not all A!)")
    void test9_CorrectAnswersValidated() {
        Course course = new Course();
        course.setId(40L);
        course.setTitle("Digital Logic Design & Verilog Modeling");

        CoursePhase phase = new CoursePhase();
        phase.setId(401L);
        phase.setCourse(course);
        phase.setPhaseNumber(1);
        phase.setTitle("Phase 1: Number Systems, Complements & Binary Codes");

        List<QuizQuestion> questions = SubjectQuestionBank.getQuestionsForPhase(phase, null);
        assertEquals(10, questions.size());

        Map<String, Integer> counts = new HashMap<>();
        for (QuizQuestion q : questions) {
            String correct = q.getCorrectOption();
            assertTrue(List.of("A", "B", "C", "D").contains(correct), "Invalid correct option: " + correct);
            counts.put(correct, counts.getOrDefault(correct, 0) + 1);
        }

        // Must have at least 3 distinct letters represented across the 10 questions
        assertTrue(counts.size() >= 3, "Answers must be distributed among A, B, C, D; got: " + counts);
        assertNotEquals(10, counts.getOrDefault("A", 0), "Answers must NOT be 100% all A!");
    }

    // =========================================================================
    // SCENARIO 10: No duplicate questions within a quiz
    // =========================================================================
    @Test
    @DisplayName("Scenario 10: All 10 questions within a quiz are unique with no duplicate questions")
    void test10_NoDuplicateQuestions() {
        Course course = new Course();
        course.setId(50L);
        course.setTitle("Engineering Physics & Semiconductor Basics");

        CoursePhase phase = new CoursePhase();
        phase.setId(501L);
        phase.setCourse(course);
        phase.setPhaseNumber(1);
        phase.setTitle("Phase 1: Wave Optics: Interference of Light & Thin Film Phenomena");

        List<QuizQuestion> questions = SubjectQuestionBank.getQuestionsForPhase(phase, null);
        assertEquals(10, questions.size());

        Set<String> texts = new HashSet<>();
        for (QuizQuestion q : questions) {
            String clean = q.getQuestionText().trim().toLowerCase();
            assertTrue(texts.add(clean), "Duplicate question found in quiz: " + q.getQuestionText());
        }
        assertEquals(10, texts.size());
    }

    // =========================================================================
    // SCENARIO 11: Branch adaptation
    // =========================================================================
    @Test
    @DisplayName("Scenario 11: Branch adaptation tailoring curricula and recommendations")
    void test11_BranchAdaptation() {
        User mechStudent = createStudent("mech@vrsec.ac.in", "MECH", "2nd Year", "1st Sem");
        User cseStudent = createStudent("cse@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");

        List<Map<String, Object>> mechRecs = learningPathService.getPersonalizedRecommendations(mechStudent);
        List<Map<String, Object>> cseRecs = learningPathService.getPersonalizedRecommendations(cseStudent);

        boolean mechHasMechanics = mechRecs.stream().anyMatch(r -> ((String) r.get("title")).contains("Mechanics of Solids"));
        boolean cseHasDSA = cseRecs.stream().anyMatch(r -> ((String) r.get("title")).contains("Data Structures"));

        assertTrue(mechHasMechanics, "MECH student should receive Mechanics of Solids recommendation");
        assertTrue(cseHasDSA, "CSE student should receive Data Structures recommendation");

        // Curricula titles for MECH Mechanics vs CSE Data Structures
        List<Map<String, Object>> mechCurriculum = SubjectCurriculumRegistry.getCurriculumForSubject("Mechanics of Solids & Strength of Materials", mechStudent);
        assertTrue(((String) mechCurriculum.get(0).get("title")).contains("Stress") || ((String) mechCurriculum.get(0).get("title")).contains("Strain"));
    }

    // =========================================================================
    // SCENARIO 12: Year and semester adaptation
    // =========================================================================
    @Test
    @DisplayName("Scenario 12: Year and semester adaptation for course curricula")
    void test12_YearAndSemesterAdaptation() {
        User firstYear = createStudent("first@vrsec.ac.in", "CSE", "1st Year", "1st Sem");
        User thirdYear = createStudent("third@vrsec.ac.in", "CSE", "3rd Year", "1st Sem");

        List<Map<String, Object>> recs1 = learningPathService.getPersonalizedRecommendations(firstYear);
        List<Map<String, Object>> recs3 = learningPathService.getPersonalizedRecommendations(thirdYear);

        boolean rec1HasFoundations = recs1.stream().anyMatch(r -> ((String) r.get("title")).contains("Engineering Mathematics") || ((String) r.get("title")).contains("Programming"));
        boolean rec3HasAdvanced = recs3.stream().anyMatch(r -> ((String) r.get("title")).contains("Operating Systems") || ((String) r.get("title")).contains("Networks"));

        assertTrue(rec1HasFoundations, "1st Year student should receive foundations");
        assertTrue(rec3HasAdvanced, "3rd Year student should receive 3rd Year core courses");
    }

    // =========================================================================
    // SCENARIO 13: Difficulty level adaptation
    // =========================================================================
    @Test
    @DisplayName("Scenario 13: Difficulty adaptation based on CGPA academic standing")
    void test13_DifficultyLevelAdaptation() {
        User highCgpa = createStudent("high@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        highCgpa.setCgpa(9.4);

        User lowCgpa = createStudent("low@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        lowCgpa.setCgpa(6.2);

        List<Map<String, Object>> highRecs = learningPathService.getPersonalizedRecommendations(highCgpa);
        List<Map<String, Object>> lowRecs = learningPathService.getPersonalizedRecommendations(lowCgpa);

        boolean highHasHonors = highRecs.stream().anyMatch(r -> "Honors AI".equals(r.get("suggestedBy")) || ((String) r.get("reason")).contains("Honors"));
        boolean lowHasRemedial = lowRecs.stream().anyMatch(r -> "Adaptive AI".equals(r.get("suggestedBy")) || ((String) r.get("reason")).contains("Academic Booster"));

        assertTrue(highHasHonors, "High CGPA student should receive Honors track recommendation");
        assertTrue(lowHasRemedial, "Low CGPA student should receive Academic Booster recommendation");
    }

    // =========================================================================
    // SCENARIO 14: Progress preservation
    // =========================================================================
    @Test
    @DisplayName("Scenario 14: Completed phases and course progress are preserved upon generating/taking quizzes")
    void test14_ProgressPreservation() {
        Course course = new Course();
        course.setId(60L);
        course.setTitle("Data Structures & Algorithmic Complexity");
        course.setProgress(25);
        course.setCurrentPhase(2);
        course.setTotalPhases(8);
        courseDb.put(60L, course);

        CoursePhase p1 = new CoursePhase();
        p1.setId(601L);
        p1.setCourse(course);
        p1.setPhaseNumber(1);
        p1.setTitle("Phase 1: Algorithmic Complexity");
        p1.setCompleted(true);
        p1.setUnlocked(true);
        phaseDb.put(601L, p1);

        CoursePhase p2 = new CoursePhase();
        p2.setId(602L);
        p2.setCourse(course);
        p2.setPhaseNumber(2);
        p2.setTitle("Phase 2: Arrays & Two Pointers");
        p2.setCompleted(false);
        p2.setUnlocked(true);
        phaseDb.put(602L, p2);

        // Fetch quiz for Phase 2
        List<Map<String, Object>> quiz = learningPathService.getOrCreatePhaseQuiz(60L, 2);
        assertEquals(10, quiz.size());

        // Verify Phase 1 is still completed and course progress is not reset to 0
        assertTrue(p1.isCompleted());
        assertEquals(25, course.getProgress());
        assertEquals(2, course.getCurrentPhase());
    }

    // =========================================================================
    // SCENARIO 15: Gemini AI fallback resiliency
    // =========================================================================
    @Test
    @DisplayName("Scenario 15: Resilient fallback when Gemini AI is offline without returning generic templates")
    void test15_GeminiAIFallbackResiliency() {
        // geminiApiKey is null/empty by default in test
        User student = createStudent("student@vrsec.ac.in", "CSE", "2nd Year", "1st Sem");
        Course course = learningPathService.generateSubjectLearningPath(student, "Mechanics of Solids & Strength of Materials");

        assertNotNull(course);
        assertEquals(6, course.getTotalPhases());

        // Verify the fallback curriculum is authentic Mechanics, NOT generic
        CoursePhase p1 = phaseDb.values().stream()
                .filter(p -> p.getCourse().getId().equals(course.getId()) && p.getPhaseNumber() == 1)
                .findFirst().orElseThrow();

        assertTrue(p1.getTitle().contains("Stress") || p1.getTitle().contains("Strain"));
        assertFalse(p1.getTitle().contains("Fundamental Building Blocks & Syntax"));

        // Generate quiz for phase 1
        List<Map<String, Object>> quiz = learningPathService.getOrCreatePhaseQuiz(course.getId(), 1);
        assertEquals(10, quiz.size());

        // Options must be non-generic
        Map<String, Object> q1 = quiz.get(0);
        List<String> options = (List<String>) q1.get("options");
        assertEquals(4, options.size());
        assertFalse(options.get(0).contains("It provides an optimized, mathematically sound architectural foundation"));
    }

    // =========================================================================
    // SCENARIO 16: Invalid AI response rejection
    // =========================================================================
    @Test
    @DisplayName("Scenario 16: validateQuizQuestions rejects invalid/mono-letter/duplicate responses")
    void test16_InvalidAIResponseRejection() {
        CoursePhase phase = new CoursePhase();
        phase.setId(701L);

        // Case A: valid questions
        List<QuizQuestion> validList = SubjectQuestionBank.getQuestionsForPhase(phase, null);
        assertTrue(learningPathService.validateQuizQuestions(validList), "Valid blueprint questions must pass validation");

        // Case B: invalid options (duplicate options)
        List<QuizQuestion> dupList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            QuizQuestion q = new QuizQuestion();
            q.setQuestionText("Question " + i);
            q.setOptionA("Same Option");
            q.setOptionB("Same Option"); // Duplicate!
            q.setOptionC("Different");
            q.setOptionD("Another");
            q.setCorrectOption(i % 2 == 0 ? "A" : "B");
            dupList.add(q);
        }
        assertFalse(learningPathService.validateQuizQuestions(dupList), "Duplicate options within a question must be rejected");

        // Case C: All 10 questions have correctOption = 'A' (mono-letter flaw)
        List<QuizQuestion> monoList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            QuizQuestion q = new QuizQuestion();
            q.setQuestionText("Question " + i);
            q.setOptionA("Opt A " + i);
            q.setOptionB("Opt B " + i);
            q.setOptionC("Opt C " + i);
            q.setOptionD("Opt D " + i);
            q.setCorrectOption("A"); // All A!
            monoList.add(q);
        }
        assertFalse(learningPathService.validateQuizQuestions(monoList), "Mono-letter answers must be rejected");

        // Case D: Fewer than 10 questions
        List<QuizQuestion> shortList = validList.subList(0, 8);
        assertFalse(learningPathService.validateQuizQuestions(shortList), "Quiz with fewer than 10 questions must be rejected");
    }
}
