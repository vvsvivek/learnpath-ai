package com.vrsec.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1}")
    private String groqApiUrl = "https://api.groq.com/openai/v1";

    @Value("${groq.model:openai/gpt-oss-120b}")
    private String groqModel = "openai/gpt-oss-120b";

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel = "gpt-4o-mini";

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private TopicPerformanceRepository topicPerformanceRepository;
    @Autowired private SuggestedCourseRepository suggestedCourseRepository;
    @Autowired private ChatSessionRepository chatSessionRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;

    @Autowired(required = false) private com.vrsec.backend.service.ai.AiProviderRouter aiProviderRouter;
    @Autowired(required = false) private com.vrsec.backend.service.ai.AiTokenBudgetManager tokenBudgetManager;
    @Autowired(required = false) private com.vrsec.backend.service.ai.AiUsageMetricsService usageMetricsService;
    @Autowired(required = false) private com.vrsec.backend.service.ai.AiRateLimitTracker rateLimitTracker;
    @Autowired(required = false) private com.vrsec.backend.service.ai.AiConcurrencyLimiter concurrencyLimiter;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Constant stable system prompt prefix to maximize Groq prompt caching hits
    public static final String STUDENT_SYSTEM_PROMPT_PREFIX =
            "You are 'LearnPath AI Student Mentor', a premier university academic tutor, coding mentor, and intelligent conversational assistant.\n"
            + "Formatting rules: Use clear GitHub-flavored markdown. Use bold headers, structured bullet points for step-by-step walkthroughs, and fully commented, syntax-highlighted code blocks for programming solutions.\n"
            + "Tone: Direct, supportive, intellectually rigorous, and pedagogical.\n\n"
            + "CORE CAPABILITIES & OPERATIONAL GUIDELINES:\n"
            + "1. GENERAL KNOWLEDGE & ACADEMIC TOPICS:\n"
            + "   - Answer all legitimate general knowledge, science, history, geography, mathematics, and engineering questions directly, accurately, and thoroughly.\n"
            + "   - Provide intuitive explanations followed by rigorous technical or conceptual depth.\n"
            + "   - Never prepend or force-feed unsolicited personal enrollment summaries, course lists, or greeting formulas into unrelated general knowledge or technical questions.\n"
            + "2. SOFTWARE ENGINEERING, CODING & ALGORITHMS:\n"
            + "   - Provide complete, fully executable, production-grade code when requested in C, C++, Java, Python, JavaScript, TypeScript, and SQL.\n"
            + "   - Include complete imports, main/driver methods, clear inline comments, and expected sample output.\n"
            + "   - For algorithms and data structures, provide detailed step-by-step logic, working code, and explicit time and space complexity analysis (Big-O notation for best, average, and worst cases).\n"
            + "   - For code debugging, pinpoint the exact line or logic error, explain why it fails, and provide the corrected code with verification.\n"
            + "3. MULTI-TURN CONVERSATION & CONTEXTUAL MEMORY:\n"
            + "   - Understand the user's conversational intent across consecutive turns in the session.\n"
            + "   - Seamlessly handle contextual follow-ups such as 'convert this into Python', 'explain the second point in more detail', 'give another example', or 'optimize this algorithm'.\n"
            + "   - Never repeat generic introductory greetings or repeat previous explanations verbatim.\n"
            + "4. PERSONALIZED ACADEMIC MENTORSHIP:\n"
            + "   - Use the verified student database records ONLY when the student asks about their enrolled courses, progress, CGPA, quiz performance, weak concepts, or personalized study plans.\n"
            + "   - Answer strictly and honestly based on the database records. Never invent or fabricate course titles, phase progress, scores, or concepts.\n"
            + "   - If no records exist for a requested item (e.g. no quizzes attempted yet), clearly state that no records are present in the university database yet.";

    public static final String FACULTY_SYSTEM_PROMPT_PREFIX =
            "You are 'LearnPath AI Faculty Advisory Engine', an executive academic intelligence and pedagogical advisory system for professors at Velagapudi Ramakrishna Siddhartha Engineering College (VRSEC).\n"
            + "Formatting rules: Use clear GitHub-flavored markdown. Use structured tables for metrics, bullet points for pedagogical recommendations, and clear section dividers.\n"
            + "Tone: Professional, collegial, data-grounded, and respectful.\n\n"
            + "PRIMARY FACULTY USE CASES:\n"
            + "1. Cohort Performance Summaries: Synthesize authorized student progress, average CGPA, course completion, and quiz analytics.\n"
            + "2. Slow Learner & Remedial Interventions: Identify students falling behind (CGPA < 6.5 or low quiz pass rate) and provide concrete, actionable remedial activities.\n"
            + "3. Assessment & Curriculum Engineering: Generate high-quality classroom assignments, diagnostic quiz questions, and structured lesson plans on demand.\n"
            + "4. Pedagogical Guidance: Provide exemplary academic explanations, classroom analogies, and interactive student exercises.\n"
            + "5. Multi-Turn Memory: Maintain conversation context across consecutive questions from the faculty member.";

    public AiAssistantService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10s connect timeout
        factory.setReadTimeout(30000);    // 30s read timeout
        this.restTemplate = new RestTemplate(factory);
    }

    // =========================================================================
    // 1. STUDENT AI ASSISTANT (Individual Role-Aware Academic & Mentorship Engine)
    // =========================================================================
    @Transactional
    public Map<String, Object> chatWithStudentAi(User student, ChatSession session, String userMessage) {
        if (student == null) {
            throw new IllegalArgumentException("Student entity required.");
        }

        ChatSession activeSession = resolveSession(session, student.getEmail(), student, "STUDENT");

        // 1. Retrieve up to the last 10 conversation turns from database BEFORE saving new user message
        List<ChatMessage> pastMessages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(activeSession.getId());

        // 2. Fetch authenticated student's real database records
        List<Course> activeCourses = courseRepository.findByUserEmailIgnoreCaseAndDeletedFalse(student.getEmail());
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByCreatedAtDesc(student.getId());
        List<TopicPerformance> weakTopics = topicPerformanceRepository.findByUserId(student.getId()).stream()
                .filter(tp -> "NEEDS_ATTENTION".equalsIgnoreCase(tp.getStatus()) || tp.getTimesIncorrect() > 0)
                .collect(Collectors.toList());

        String studentContext = buildStudentContext(student, activeCourses, attempts, weakTopics);

        // 3. Construct dedicated Student System Instruction
        String systemInstruction = "You are 'LearnPath AI Student Mentor', a premier university academic tutor, coding mentor, and intelligent conversational assistant.\n"
                + "Formatting rules: Use clear GitHub-flavored markdown. Use bold headers, structured bullet points for step-by-step walkthroughs, and fully commented, syntax-highlighted code blocks for programming solutions.\n"
                + "Tone: Direct, supportive, intellectually rigorous, and pedagogical.\n\n"
                + "CORE CAPABILITIES & OPERATIONAL GUIDELINES:\n"
                + "1. GENERAL KNOWLEDGE & ACADEMIC TOPICS:\n"
                + "   - Answer all legitimate general knowledge, science, history, geography, mathematics, and engineering questions directly, accurately, and thoroughly.\n"
                + "   - Provide intuitive explanations followed by rigorous technical or conceptual depth.\n"
                + "   - Never prepend or force-feed unsolicited personal enrollment summaries, course lists, or greeting formulas into unrelated general knowledge or technical questions.\n"
                + "2. SOFTWARE ENGINEERING, CODING & ALGORITHMS:\n"
                + "   - Provide complete, fully executable, production-grade code when requested in C, C++, Java, Python, JavaScript, TypeScript, and SQL.\n"
                + "   - Include complete imports, main/driver methods, clear inline comments, and expected sample output.\n"
                + "   - For algorithms and data structures, provide detailed step-by-step logic, working code, and explicit time and space complexity analysis (Big-O notation for best, average, and worst cases).\n"
                + "   - For code debugging, pinpoint the exact line or logic error, explain why it fails, and provide the corrected code with verification.\n"
                + "3. MULTI-TURN CONVERSATION & CONTEXTUAL MEMORY:\n"
                + "   - Understand the user's conversational intent across consecutive turns in the session.\n"
                + "   - Seamlessly handle contextual follow-ups such as 'convert this into Python', 'explain the second point in more detail', 'give another example', or 'optimize this algorithm'.\n"
                + "   - Never repeat generic introductory greetings or repeat previous explanations verbatim.\n"
                + "4. PERSONALIZED ACADEMIC MENTORSHIP:\n"
                + "   - Use the verified student database records below ONLY when the student asks about their enrolled courses, progress, CGPA, quiz performance, weak concepts, or personalized study plans.\n"
                + "   - Answer strictly and honestly based on the database records. Never invent or fabricate course titles, phase progress, scores, or concepts.\n"
                + "   - If no records exist for a requested item (e.g. no quizzes attempted yet), clearly state that no records are present in the university database yet.\n\n"
                + "=== VERIFIED STUDENT DATABASE RECORDS ===\n" + studentContext + "\n=========================================";

        // 4. Build multi-turn conversational contents using token budget manager
        List<Map<String, Object>> contents;
        if (tokenBudgetManager != null) {
            contents = tokenBudgetManager.buildOptimizedConversation(pastMessages, userMessage, studentContext);
        } else {
            contents = buildConversationContents(pastMessages, userMessage);
        }

        // 5. Save incoming user message (preserved in DB even if downstream call fails)
        ChatMessage userMsg = new ChatMessage(activeSession, "user", userMessage);
        chatMessageRepository.save(userMsg);

        // 6. Call AI API via provider router with token budgeting and rate limit backoff
        AiResponse aiResponse;
        if (aiProviderRouter != null) {
            int maxTokens = tokenBudgetManager != null ? tokenBudgetManager.getMaxCompletionTokens() : 1024;
            com.vrsec.backend.service.ai.AiProviderRequest req = new com.vrsec.backend.service.ai.AiProviderRequest(
                    STUDENT_SYSTEM_PROMPT_PREFIX,
                    studentContext,
                    contents,
                    groqModel,
                    maxTokens,
                    0.7,
                    student.getEmail()
            );
            com.vrsec.backend.service.ai.AiProviderResponse pResp = aiProviderRouter.executeChat(req);
            aiResponse = new AiResponse(pResp.isSuccess(), pResp.getText(), pResp.getModelVersion(), pResp.getErrorMessage());
        } else {
            aiResponse = callAiApi(systemInstruction, contents);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", activeSession.getId());

        if (aiResponse.isSuccess()) {
            String aiReply = aiResponse.getText();

            // Save AI reply in DB
            ChatMessage aiMsg = new ChatMessage(activeSession, "ai", aiReply);
            chatMessageRepository.save(aiMsg);

            // Auto-update session title if default
            updateSessionTitleIfNeeded(activeSession, userMessage);

            response.put("title", activeSession.getTitle());
            response.put("reply", aiReply);
            response.put("message", aiReply);
            response.put("modelVersion", aiResponse.getModelVersion());
            response.put("isError", false);
        } else {
            // Transparent, retryable error response (NO fake static fallbacks pretending to be AI)
            String errorReply = "⚠️ The LearnPath AI service is temporarily unavailable or experiencing high traffic (" +
                    aiResponse.getErrorMessage() + "). Please retry your question in a moment.";
            response.put("title", activeSession.getTitle());
            response.put("reply", errorReply);
            response.put("message", errorReply);
            response.put("isError", true);
            response.put("error", aiResponse.getErrorMessage());
        }

        return response;
    }

    // Overload for backward compatibility
    @Transactional
    public Map<String, Object> chatWithStudentAi(String email, Long sessionId, String userMessage) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        User student = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        ChatSession session = null;
        if (sessionId != null) {
            session = chatSessionRepository.findById(sessionId).orElse(null);
        }
        return chatWithStudentAi(student, session, userMessage);
    }

    private String buildStudentContext(User student, List<Course> courses, List<QuizAttempt> attempts, List<TopicPerformance> weakTopics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Student Name: ").append(student.getUsername() != null ? student.getUsername() : "Student").append("\n");
        sb.append("Branch: ").append(student.getBranch() != null ? student.getBranch() : "CSE");
        sb.append(" | Year: ").append(student.getAcademicYear() != null ? student.getAcademicYear() : "Current");
        sb.append(" | Semester: ").append(student.getSemester() != null ? student.getSemester() : "Current").append("\n");
        sb.append("Current CGPA: ").append(student.getCgpa() != null && student.getCgpa() > 0 ? student.getCgpa() + " / 10.0" : "Not yet recorded").append("\n");

        sb.append("\nEnrolled Courses (").append(courses.size()).append("):\n");
        if (courses.isEmpty()) {
            sb.append("- None currently enrolled.\n");
        } else {
            for (Course c : courses) {
                sb.append("- ").append(c.getTitle())
                  .append(" (Progress: ").append(c.getProgress() != null ? c.getProgress() : 0).append("%")
                  .append(", Current Phase: ").append(c.getCurrentPhase() != null ? c.getCurrentPhase() : 1)
                  .append("/").append(c.getTotalPhases() != null ? c.getTotalPhases() : 4).append(")\n");

                // Include phase completion breakdown
                List<CoursePhase> phases = phaseRepository.findByCourseIdOrderByPhaseNumberAsc(c.getId());
                for (CoursePhase cp : phases) {
                    sb.append("   * Phase ").append(cp.getPhaseNumber()).append(": ").append(cp.getTitle())
                      .append(" [").append(cp.isCompleted() ? "COMPLETED" : (cp.isUnlocked() ? "IN PROGRESS" : "LOCKED")).append("]\n");
                }
            }
        }

        sb.append("\nRecent Quiz Attempts (").append(attempts.size()).append("):\n");
        if (attempts.isEmpty()) {
            sb.append("- No quizzes attempted yet.\n");
        } else {
            for (QuizAttempt a : attempts.stream().limit(5).collect(Collectors.toList())) {
                sb.append("- Topic: ").append(a.getTopic() != null ? a.getTopic() : "Phase Assessment")
                  .append(" | Score: ").append(a.getScore() != null ? a.getScore() : 0).append("/").append(a.getTotalQuestions() != null ? a.getTotalQuestions() : 10)
                  .append(" (").append(String.format("%.1f", a.getPercentage() != null ? a.getPercentage() : 0.0)).append("% - ")
                  .append(Boolean.TRUE.equals(a.getPassed()) ? "PASSED" : "FAILED").append(")\n");
            }
        }

        sb.append("\nIdentified Weak Topics / Mistakes (").append(weakTopics.size()).append("):\n");
        if (weakTopics.isEmpty()) {
            sb.append("- None currently detected in recent quiz evaluations.\n");
        } else {
            for (TopicPerformance tp : weakTopics) {
                sb.append("- ").append(tp.getCourseTitle()).append(" -> ").append(tp.getConceptTag())
                  .append(" (Tested: ").append(tp.getTimesTested()).append(", Incorrect: ").append(tp.getTimesIncorrect())
                  .append(", Status: ").append(tp.getStatus()).append(")\n");
            }
        }

        return sb.toString();
    }

    // =========================================================================
    // 2. FACULTY AI ASSISTANT (Department-Scoped Cohort Intelligence Engine)
    // =========================================================================
    @Transactional
    public Map<String, Object> chatWithFacultyAi(User faculty, ChatSession session, String facultyMessage) {
        if (faculty == null) {
            throw new IllegalArgumentException("Faculty entity required.");
        }
        if (!faculty.isFaculty()) {
            throw new SecurityException("Unauthorized: Only verified faculty accounts may access the Faculty AI Hub.");
        }

        ChatSession activeSession = resolveSession(session, faculty.getEmail(), faculty, "FACULTY");

        // 1. Retrieve up to the last 10 conversation turns from database
        List<ChatMessage> pastMessages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(activeSession.getId());

        // 2. Retrieve authorized student cohort strictly scoped to faculty authorization rules
        // SEPARATION OF AUTHORIZATION DUTIES:
        // 1. Course Request Approval: Covered by AuthService.isAuthorizedCourseApprover (NOT checked here).
        // 2. Institution-Wide Analytics: STRICTLY restricted to verified administrators (ADMIN).
        //    Department HODs and course approvers do NOT receive institution-wide analytics access.
        // 3. Department Analytics: Ordinary faculty and Departmental HODs are scoped STRICTLY
        //    to their assigned department (faculty.getBranch()).
        boolean isInstitutionalAdmin = "ADMIN".equalsIgnoreCase(faculty.getRole());

        String facultyDept = faculty.getBranch() != null && !faculty.getBranch().trim().isEmpty() ? faculty.getBranch().trim().toUpperCase() : null;

        // STUDENT FILTERING:
        // - Role must be STUDENT.
        // - Verified identity.
        // - NO email domain restrictions: Students registering with Gmail, Outlook, vrsec.ac.in, or any valid provider
        //   are all included when they belong to the authorized department cohort.
        List<User> authorizedStudents = userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> u.getEmail() != null && !u.getEmail().trim().isEmpty())
                .filter(u -> {
                    if (isInstitutionalAdmin) {
                        return true;
                    }
                    if (facultyDept == null || "GENERAL".equalsIgnoreCase(facultyDept)) {
                        return false; // Unassigned faculty cannot view student records
                    }
                    return u.getBranch() != null && u.getBranch().trim().equalsIgnoreCase(facultyDept);
                })
                .collect(Collectors.toList());

        List<String> studentEmails = authorizedStudents.stream().map(User::getEmail).collect(Collectors.toList());
        List<Course> cohortCourses = studentEmails.isEmpty() ? Collections.emptyList() :
                courseRepository.findByUserEmailIn(studentEmails).stream().filter(c -> !c.isDeleted()).collect(Collectors.toList());
        List<QuizAttempt> cohortAttempts = studentEmails.isEmpty() ? Collections.emptyList() :
                quizAttemptRepository.findAll().stream()
                        .filter(a -> a.getUser() != null && studentEmails.contains(a.getUser().getEmail()))
                        .collect(Collectors.toList());

        List<SuggestedCourse> recommendations = suggestedCourseRepository.findByFacultyEmail(faculty.getEmail());

        String analyticsData = buildFacultyAnalyticsContext(faculty, facultyDept != null ? facultyDept : "UNASSIGNED", isInstitutionalAdmin, authorizedStudents, cohortCourses, cohortAttempts, recommendations);

        // 3. Construct dedicated Faculty System Instruction
        String systemInstruction = "You are 'LearnPath AI Faculty Advisory Engine', an executive academic intelligence and pedagogical advisory system for professors at Velagapudi Ramakrishna Siddhartha Engineering College (VRSEC).\n"
                + "Formatting rules: Use clear GitHub-flavored markdown. Use structured tables for metrics, bullet points for pedagogical recommendations, and clear section dividers.\n"
                + "Tone: Professional, collegial, data-grounded, and respectful.\n\n"
                + "SECURITY & AUTHORIZATION BOUNDARY:\n"
                + (isInstitutionalAdmin
                    ? "- You are operating under INSTITUTIONAL ADMINISTRATOR scope with access to all academic departments across the university.\n"
                    : "- You are authorized to analyze ONLY verified students in this faculty member's assigned department cohort: [" + (facultyDept != null ? facultyDept : "UNASSIGNED") + "].\n"
                    + "- Departmental HODs and faculty accounts must NEVER be allowed access to student records outside their authorized department.\n"
                    + "- If the faculty member asks about an unauthorized department or cohort, decline respectfully citing departmental data privacy policies.\n")
                + "- If cohort mapping is unavailable or limited, report that limitation transparently.\n\n"
                + "PRIMARY FACULTY USE CASES:\n"
                + "1. Cohort Performance Summaries: Synthesize authorized student progress, average CGPA, course completion, and quiz analytics.\n"
                + "2. Slow Learner & Remedial Interventions: Identify students falling behind (CGPA < 6.5 or low quiz pass rate) and provide concrete, actionable remedial activities (e.g. structured peer-learning sessions, targeted practice problem sets, milestone reviews for subjects like DBMS, DSA, OS).\n"
                + "3. Assessment & Curriculum Engineering: Generate high-quality classroom assignments, diagnostic quiz questions (with options, answer keys, and pedagogical rationales), and structured lesson plans on demand.\n"
                + "4. Pedagogical Guidance: When asked general academic or teaching questions (e.g. 'Explain database normalization', 'Suggest teaching strategies for recursion'), provide exemplary academic explanations, classroom analogies, and interactive student exercises.\n"
                + "5. Multi-Turn Memory: Maintain conversation context across consecutive questions from the faculty member.\n\n"
                + "=== VERIFIED AUTHORIZED COHORT DATABASE ANALYTICS ===\n" + analyticsData + "\n=====================================================";

        // 4. Build multi-turn conversational contents using token budget manager
        List<Map<String, Object>> contents;
        if (tokenBudgetManager != null) {
            contents = tokenBudgetManager.buildOptimizedConversation(pastMessages, facultyMessage, analyticsData);
        } else {
            contents = buildConversationContents(pastMessages, facultyMessage);
        }

        // 5. Save incoming faculty query
        ChatMessage userMsg = new ChatMessage(activeSession, "user", facultyMessage);
        chatMessageRepository.save(userMsg);

        // 6. Call AI API via provider router with token budgeting and rate limit backoff
        AiResponse aiResponse;
        if (aiProviderRouter != null) {
            int maxTokens = tokenBudgetManager != null ? tokenBudgetManager.getMaxCompletionTokens() : 1024;
            com.vrsec.backend.service.ai.AiProviderRequest req = new com.vrsec.backend.service.ai.AiProviderRequest(
                    FACULTY_SYSTEM_PROMPT_PREFIX,
                    analyticsData,
                    contents,
                    groqModel,
                    maxTokens,
                    0.7,
                    faculty.getEmail()
            );
            com.vrsec.backend.service.ai.AiProviderResponse pResp = aiProviderRouter.executeChat(req);
            aiResponse = new AiResponse(pResp.isSuccess(), pResp.getText(), pResp.getModelVersion(), pResp.getErrorMessage());
        } else {
            aiResponse = callAiApi(systemInstruction, contents);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", activeSession.getId());

        if (aiResponse.isSuccess()) {
            String aiReply = aiResponse.getText();

            ChatMessage aiMsg = new ChatMessage(activeSession, "ai", aiReply);
            chatMessageRepository.save(aiMsg);

            updateSessionTitleIfNeeded(activeSession, facultyMessage);

            response.put("title", activeSession.getTitle());
            response.put("reply", aiReply);
            response.put("message", aiReply);
            response.put("modelVersion", aiResponse.getModelVersion());
            response.put("isError", false);
        } else {
            String errorReply = "⚠️ The LearnPath Faculty AI Advisor is temporarily unavailable or experiencing high traffic (" +
                    aiResponse.getErrorMessage() + "). Please retry your request in a moment.";
            response.put("title", activeSession.getTitle());
            response.put("reply", errorReply);
            response.put("message", errorReply);
            response.put("isError", true);
            response.put("error", aiResponse.getErrorMessage());
        }

        return response;
    }

    // Overload for backward compatibility
    @Transactional
    public Map<String, Object> chatWithFacultyAi(String facultyEmail, Long sessionId, String facultyMessage) {
        String cleanEmail = facultyEmail != null ? facultyEmail.trim().toLowerCase() : "";
        User faculty = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Faculty member not found"));
        ChatSession session = null;
        if (sessionId != null) {
            session = chatSessionRepository.findById(sessionId).orElse(null);
        }
        return chatWithFacultyAi(faculty, session, facultyMessage);
    }

    private String buildFacultyAnalyticsContext(
            User faculty,
            String department,
            boolean isInstitutionalAdmin,
            List<User> cohortStudents,
            List<Course> courses,
            List<QuizAttempt> attempts,
            List<SuggestedCourse> recommendations) {

        StringBuilder sb = new StringBuilder();
        sb.append("Faculty Name: ").append(faculty.getUsername() != null ? faculty.getUsername() : "Professor").append("\n");
        sb.append("Assigned Department: ").append(department).append("\n");
        sb.append("Authorization Level: ").append(isInstitutionalAdmin ? "Institutional / Administrator Scope (All Departments)" : "Department Cohort Scope (" + department + ")\n");
        sb.append("Total Authorized Students in Cohort: ").append(cohortStudents.size()).append("\n");

        if (cohortStudents.isEmpty()) {
            sb.append("\nNote: No verified students currently enrolled in the ").append(department).append(" department.\n");
            return sb.toString();
        }

        // Academic Year breakdown
        Map<String, Long> yearCount = cohortStudents.stream()
                .collect(Collectors.groupingBy(u -> u.getAcademicYear() != null ? u.getAcademicYear() : "Unassigned", Collectors.counting()));
        sb.append("Cohort Year Distribution: ").append(yearCount).append("\n");

        // Average CGPA
        double avgCgpa = cohortStudents.stream()
                .filter(u -> u.getCgpa() != null && u.getCgpa() > 0)
                .mapToDouble(User::getCgpa)
                .average()
                .orElse(0.0);
        sb.append(String.format("Cohort Average CGPA: %.2f / 10.0\n", avgCgpa));

        // Course completion metrics
        long totalEnrolled = courses.size();
        long completedCourses = courses.stream().filter(c -> c.getProgress() != null && c.getProgress() == 100).count();
        double avgProgress = courses.isEmpty() ? 0.0 : courses.stream().mapToInt(c -> c.getProgress() != null ? c.getProgress() : 0).average().orElse(0.0);
        sb.append("Active Enrolled Courses in Cohort: ").append(totalEnrolled).append("\n");
        sb.append("Completed Courses: ").append(completedCourses).append("\n");
        sb.append(String.format("Average Course Progress: %.1f%%\n", avgProgress));

        // Quiz pass rate
        long totalQuizzes = attempts.size();
        long passedQuizzes = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();
        double passRate = totalQuizzes > 0 ? ((double) passedQuizzes / totalQuizzes) * 100.0 : 0.0;
        sb.append("Total Quiz Evaluations: ").append(totalQuizzes).append("\n");
        sb.append(String.format("Cohort Quiz Pass Rate: %.1f%% (%d passed / %d attempts)\n", passRate, passedQuizzes, totalQuizzes));

        // Identify students needing additional remedial support (CGPA < 6.5 or low quiz pass rate)
        List<User> needsSupport = cohortStudents.stream()
                .filter(u -> u.getCgpa() != null && u.getCgpa() > 0 && u.getCgpa() < 6.5)
                .collect(Collectors.toList());
        sb.append("\nStudents Requiring Additional Support (CGPA < 6.5) in Cohort: ").append(needsSupport.size()).append("\n");
        for (User u : needsSupport) {
            sb.append("- ").append(u.getUsername()).append(" (").append(u.getEmail()).append(")")
              .append(" | Year: ").append(u.getAcademicYear() != null ? u.getAcademicYear() : "N/A")
              .append(" | CGPA: ").append(u.getCgpa()).append("\n");
        }

        // Active Faculty Course Recommendations
        sb.append("\nYour Active Department Course Recommendations (").append(recommendations.size()).append("):\n");
        if (recommendations.isEmpty()) {
            sb.append("- No department course recommendations published yet.\n");
        } else {
            for (SuggestedCourse sc : recommendations) {
                sb.append("- ").append(sc.getTitle()).append(" (Target: ").append(sc.getTargetBranch())
                  .append(" ").append(sc.getTargetYear() != null ? sc.getTargetYear() : "")
                  .append(", Priority: ").append(sc.getPriority()).append(")\n");
            }
        }

        return sb.toString();
    }

    // =========================================================================
    // 3. MULTI-TURN CONVERSATION HISTORY BUILDER (OpenAI Responses API Compatible)
    // =========================================================================
    private List<Map<String, Object>> buildConversationContents(List<ChatMessage> pastMessages, String newUserMessage) {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (pastMessages != null && !pastMessages.isEmpty()) {
            // Keep up to the last 10 historical messages (excluding the new one)
            int startIdx = Math.max(0, pastMessages.size() - 10);
            List<ChatMessage> recent = pastMessages.subList(startIdx, pastMessages.size());

            String lastRole = null;
            for (ChatMessage m : recent) {
                String sender = m.getSender();
                String role = (sender != null && sender.equalsIgnoreCase("user")) ? "user" : "assistant";
                String text = m.getContent();
                if (text == null || text.trim().isEmpty()) continue;

                // Skip consecutive duplicate roles
                if (role.equals(lastRole)) {
                    continue;
                }

                // If history starts with assistant (e.g. initial greeting), skip so first turn is user
                if (contents.isEmpty() && "assistant".equals(role)) {
                    continue;
                }

                contents.add(Map.of(
                        "role", role,
                        "content", text.trim()
                ));
                lastRole = role;
            }
        }

        // Append the incoming user message as the final 'user' turn
        contents.add(Map.of(
                "role", "user",
                "content", newUserMessage.trim()
        ));

        return contents;
    }

    // =========================================================================
    // 4. AI API CLIENT ROUTER & GROQ / OPENAI RESPONSES API IMPLEMENTATION
    // =========================================================================
    private AiResponse callAiApi(String systemInstruction, List<Map<String, Object>> contents) {
        // Priority 1: Groq OpenAI-Compatible API (primary production/staging engine)
        if (groqApiKey != null && !groqApiKey.trim().isEmpty() && !groqApiKey.contains("YOUR_")) {
            return callGroqApi(systemInstruction, contents);
        }

        // Priority 2: OpenAI Responses API fallback
        if (openaiApiKey != null && !openaiApiKey.trim().isEmpty() && !openaiApiKey.contains("YOUR_")) {
            return callOpenAiApi(systemInstruction, contents);
        }

        // Priority 3: Gemini API fallback if configured
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.contains("YOUR_")) {
            return callGeminiApi(systemInstruction, contents);
        }

        return new AiResponse(false, null, null, "AI engine is not configured (Groq/OpenAI/Gemini API key missing)");
    }

    private String resolveGroqChatUrl() {
        String baseUrl = (groqApiUrl != null && !groqApiUrl.trim().isEmpty()) ? groqApiUrl.trim() : "https://api.groq.com/openai/v1";
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat/completions";
        }
        return baseUrl + "/chat/completions";
    }

    /**
     * Groq OpenAI-Compatible Chat Completions Client (POST https://api.groq.com/openai/v1/chat/completions)
     * High-throughput inference engine supporting llama-3.3-70b-versatile.
     */
    private AiResponse callGroqApi(String systemInstruction, List<Map<String, Object>> contents) {
        if (groqApiKey == null || groqApiKey.trim().isEmpty() || groqApiKey.contains("YOUR_")) {
            return new AiResponse(false, null, null, "Groq API key is not configured");
        }

        String effectiveModel = (groqModel != null && !groqModel.trim().isEmpty()) ? groqModel.trim() : "openai/gpt-oss-120b";
        String chatUrl = resolveGroqChatUrl();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey.trim());

            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
                messages.add(Map.of("role", "system", "content", systemInstruction.trim()));
            }
            if (contents != null) {
                messages.addAll(contents);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", effectiveModel);
            payload.put("messages", messages);
            payload.put("temperature", 0.7);
            payload.put("max_tokens", 4096);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            Map response = restTemplate.postForObject(chatUrl, request, Map.class);

            String extractedText = extractTextFromOpenAiResponse(response);
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                String modelVersion = (response != null && response.containsKey("model"))
                        ? String.valueOf(response.get("model"))
                        : effectiveModel;
                return new AiResponse(true, extractedText.trim(), modelVersion, null);
            }
            return new AiResponse(false, null, null, "Empty response received from Groq (" + effectiveModel + ")");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorMsg = "HTTP " + e.getStatusCode().value() + " " + e.getStatusText();
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    Map errJson = objectMapper.readValue(responseBody, Map.class);
                    if (errJson != null && errJson.containsKey("error")) {
                        Object errObj = errJson.get("error");
                        if (errObj instanceof Map) {
                            Map errMap = (Map) errObj;
                            if (errMap.containsKey("message")) {
                                errorMsg = String.valueOf(errMap.get("message"));
                            }
                        } else if (errObj instanceof String) {
                            errorMsg = (String) errObj;
                        }
                    }
                }
            } catch (Exception ignored) {}
            System.err.println("[GROQ AI] Error (" + effectiveModel + "): " + errorMsg);
            return new AiResponse(false, null, null, errorMsg);
        } catch (Exception e) {
            System.err.println("[GROQ AI] Exception (" + effectiveModel + "): " + e.getMessage());
            return new AiResponse(false, null, null, e.getMessage());
        }
    }

    /**
     * Official OpenAI Responses API Client (POST https://api.openai.com/v1/responses)
     * Fully supports gpt-4o-mini (default) and gpt-4o (configured via OPENAI_MODEL).
     */
    private AiResponse callOpenAiApi(String systemInstruction, List<Map<String, Object>> contents) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty() || openaiApiKey.contains("YOUR_")) {
            return new AiResponse(false, null, null, "AI engine is not configured (Groq/OpenAI/Gemini API key missing)");
        }

        String effectiveModel = (openaiModel != null && !openaiModel.trim().isEmpty()) ? openaiModel.trim() : "gpt-4o-mini";
        String apiUrl = "https://api.openai.com/v1/responses";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey.trim());

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", effectiveModel);
            if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
                payload.put("instructions", systemInstruction.trim());
            }
            payload.put("input", contents);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            Map response = restTemplate.postForObject(apiUrl, request, Map.class);

            String extractedText = extractTextFromOpenAiResponse(response);
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                String modelVersion = (response != null && response.containsKey("model"))
                        ? String.valueOf(response.get("model"))
                        : effectiveModel;
                return new AiResponse(true, extractedText.trim(), modelVersion, null);
            }
            return new AiResponse(false, null, null, "Empty response received from OpenAI (" + effectiveModel + ")");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorMsg = "HTTP " + e.getStatusCode().value() + " " + e.getStatusText();
            try {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    Map errJson = objectMapper.readValue(responseBody, Map.class);
                    if (errJson != null && errJson.containsKey("error")) {
                        Object errObj = errJson.get("error");
                        if (errObj instanceof Map) {
                            Map errMap = (Map) errObj;
                            if (errMap.containsKey("message")) {
                                errorMsg = String.valueOf(errMap.get("message"));
                            }
                        } else if (errObj instanceof String) {
                            errorMsg = (String) errObj;
                        }
                    }
                }
            } catch (Exception ignored) {}
            System.err.println("[OPENAI AI] Error (" + effectiveModel + "): " + errorMsg);
            return new AiResponse(false, null, null, errorMsg);
        } catch (Exception e) {
            System.err.println("[OPENAI AI] Exception (" + effectiveModel + "): " + e.getMessage());
            return new AiResponse(false, null, null, e.getMessage());
        }
    }

    /**
     * Extracts text cleanly from OpenAI Responses API responses or legacy Chat Completions responses.
     */
    public static String extractTextFromOpenAiResponse(Map response) {
        if (response == null) return null;

        // 1. Direct top-level output_text check
        if (response.containsKey("output_text") && response.get("output_text") != null) {
            String text = String.valueOf(response.get("output_text")).trim();
            if (!text.isEmpty()) return text;
        }

        // 2. Traversal of 'output' array (OpenAI Responses API standard)
        if (response.containsKey("output") && response.get("output") instanceof List) {
            List<?> outputs = (List<?>) response.get("output");
            StringBuilder sb = new StringBuilder();
            for (Object outObj : outputs) {
                if (outObj instanceof Map) {
                    Map<?, ?> outMap = (Map<?, ?>) outObj;
                    if (outMap.containsKey("content") && outMap.get("content") instanceof List) {
                        List<?> contents = (List<?>) outMap.get("content");
                        for (Object cObj : contents) {
                            if (cObj instanceof Map) {
                                Map<?, ?> cMap = (Map<?, ?>) cObj;
                                if (cMap.containsKey("text") && cMap.get("text") != null) {
                                    sb.append(cMap.get("text"));
                                }
                            }
                        }
                    } else if (outMap.containsKey("text") && outMap.get("text") != null) {
                        sb.append(outMap.get("text"));
                    }
                }
            }
            String extracted = sb.toString().trim();
            if (!extracted.isEmpty()) return extracted;
        }

        // 3. Fallback for 'choices' (legacy chat format)
        if (response.containsKey("choices") && response.get("choices") instanceof List) {
            List<?> choices = (List<?>) response.get("choices");
            if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                if (firstChoice.containsKey("message") && firstChoice.get("message") instanceof Map) {
                    Map<?, ?> msg = (Map<?, ?>) firstChoice.get("message");
                    if (msg.containsKey("content") && msg.get("content") != null) {
                        String text = String.valueOf(msg.get("content")).trim();
                        if (!text.isEmpty()) return text;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Fallback client for Google Gemini API.
     */
    private AiResponse callGeminiApi(String systemInstruction, List<Map<String, Object>> contents) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.contains("YOUR_")) {
            return new AiResponse(false, null, null, "Gemini API key is not configured");
        }

        // Map OpenAI-style contents to Gemini parts format
        List<Map<String, Object>> geminiContents = new ArrayList<>();
        for (Map<String, Object> c : contents) {
            String role = "assistant".equals(c.get("role")) ? "model" : "user";
            String text = String.valueOf(c.get("content"));
            geminiContents.add(Map.of("role", role, "parts", Collections.singletonList(Map.of("text", text))));
        }

        String[] candidateModels = {"gemini-3.8-flash", "gemini-3.5-flash-lite"};
        String lastError = null;

        for (String model : candidateModels) {
            try {
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("x-goog-api-key", geminiApiKey.trim());

                Map<String, Object> payload = new HashMap<>();
                if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
                    payload.put("systemInstruction", Map.of(
                            "parts", Collections.singletonList(Map.of("text", systemInstruction.trim()))
                    ));
                }
                payload.put("contents", geminiContents);
                payload.put("generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 4096
                ));

                HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
                Map response = restTemplate.postForObject(apiUrl, request, Map.class);

                if (response != null && response.containsKey("candidates")) {
                    List candidates = (List) response.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map first = (Map) candidates.get(0);
                        Map content = (Map) first.get("content");
                        if (content != null && content.containsKey("parts")) {
                            List parts = (List) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                String text = (String) ((Map) parts.get(0)).get("text");
                                if (text != null && !text.trim().isEmpty()) {
                                    String modelVersion = response.containsKey("modelVersion") ?
                                            String.valueOf(response.get("modelVersion")) : model;
                                    return new AiResponse(true, text.trim(), modelVersion, null);
                                }
                            }
                        }
                    }
                }
                lastError = "Empty response received from " + model;
            } catch (Exception e) {
                lastError = model + " failed: " + e.getMessage();
                System.err.println("[GEMINI AI] " + model + " call error: " + e.getMessage());
            }
        }

        return new AiResponse(false, null, null, lastError != null ? lastError : "All Gemini model candidates failed");
    }

    /**
     * Verifies model access using a minimal non-sensitive prompt without exposing the key.
     */
    public AiResponse testModelAccess(String modelName) {
        if (aiProviderRouter != null) {
            com.vrsec.backend.service.ai.AiProviderResponse pResp = aiProviderRouter.testConnection(modelName);
            return new AiResponse(pResp.isSuccess(), pResp.getText(), pResp.getModelVersion(), pResp.getErrorMessage());
        }

        if (groqApiKey != null && !groqApiKey.trim().isEmpty() && !groqApiKey.contains("YOUR_")) {
            String targetModel = (modelName != null && !modelName.trim().isEmpty())
                    ? modelName.trim()
                    : ((groqModel != null && !groqModel.trim().isEmpty()) ? groqModel.trim() : "openai/gpt-oss-120b");
            try {
                String chatUrl = resolveGroqChatUrl();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey.trim());

                Map<String, Object> payload = Map.of(
                        "model", targetModel,
                        "messages", Collections.singletonList(
                                Map.of("role", "user", "content", "Reply with exactly: Groq connection successful.")
                        ),
                        "max_tokens", 100
                );
                HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
                Map response = restTemplate.postForObject(chatUrl, request, Map.class);
                String text = extractTextFromOpenAiResponse(response);
                if (text != null && !text.trim().isEmpty()) {
                    String modelVersion = (response != null && response.containsKey("model"))
                            ? String.valueOf(response.get("model"))
                            : targetModel;
                    return new AiResponse(true, text.trim(), modelVersion, null);
                }
                return new AiResponse(false, null, null, "Empty response from Groq test probe");
            } catch (org.springframework.web.client.HttpStatusCodeException e) {
                String errorMsg = "HTTP " + e.getStatusCode().value() + " " + e.getStatusText();
                try {
                    String body = e.getResponseBodyAsString();
                    if (body != null && !body.trim().isEmpty()) {
                        Map errJson = objectMapper.readValue(body, Map.class);
                        if (errJson != null && errJson.containsKey("error")) {
                            Object errObj = errJson.get("error");
                            if (errObj instanceof Map) {
                                errorMsg = String.valueOf(((Map) errObj).getOrDefault("message", errorMsg));
                            }
                        }
                    }
                } catch (Exception ignored) {}
                return new AiResponse(false, null, null, errorMsg);
            } catch (Exception e) {
                return new AiResponse(false, null, null, e.getMessage());
            }
        }

        String targetModel = (modelName != null && !modelName.trim().isEmpty())
                ? modelName.trim()
                : ((openaiModel != null && !openaiModel.trim().isEmpty()) ? openaiModel.trim() : "gpt-4o-mini");

        if (openaiApiKey != null && !openaiApiKey.trim().isEmpty() && !openaiApiKey.contains("YOUR_")) {
            try {
                String apiUrl = "https://api.openai.com/v1/responses";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(openaiApiKey.trim());

                Map<String, Object> payload = Map.of(
                        "model", targetModel,
                        "input", Collections.singletonList(
                                Map.of("role", "user", "content", "Reply with exactly: OpenAI connection successful.")
                        )
                );
                HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
                Map response = restTemplate.postForObject(apiUrl, request, Map.class);
                String text = extractTextFromOpenAiResponse(response);
                if (text != null && !text.trim().isEmpty()) {
                    String modelVersion = (response != null && response.containsKey("model"))
                            ? String.valueOf(response.get("model"))
                            : targetModel;
                    return new AiResponse(true, text.trim(), modelVersion, null);
                }
                return new AiResponse(false, null, null, "Empty response from OpenAI test probe");
            } catch (org.springframework.web.client.HttpStatusCodeException e) {
                String errorMsg = "HTTP " + e.getStatusCode().value() + " " + e.getStatusText();
                try {
                    String body = e.getResponseBodyAsString();
                    if (body != null && !body.trim().isEmpty()) {
                        Map errJson = objectMapper.readValue(body, Map.class);
                        if (errJson != null && errJson.containsKey("error")) {
                            Object errObj = errJson.get("error");
                            if (errObj instanceof Map) {
                                errorMsg = String.valueOf(((Map) errObj).getOrDefault("message", errorMsg));
                            }
                        }
                    }
                } catch (Exception ignored) {}
                return new AiResponse(false, null, null, errorMsg);
            } catch (Exception e) {
                return new AiResponse(false, null, null, e.getMessage());
            }
        }

        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.contains("YOUR_")) {
            return callGeminiApi(null, Collections.singletonList(Map.of("role", "user", "content", "ping")));
        }

        return new AiResponse(false, null, null, "AI engine is not configured (Groq/OpenAI/Gemini API key missing)");
    }

    public Map<String, Object> getAiEngineStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        if (aiProviderRouter != null) {
            status.putAll(aiProviderRouter.getProvidersStatus());
        } else {
            boolean hasGroqKey = (groqApiKey != null && !groqApiKey.trim().isEmpty() && !groqApiKey.contains("YOUR_"));
            boolean hasOpenAiKey = (openaiApiKey != null && !openaiApiKey.trim().isEmpty() && !openaiApiKey.contains("YOUR_"));
            boolean hasGeminiKey = (geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.contains("YOUR_"));
            status.put("activeEngine", hasGroqKey ? "GROQ" : (hasOpenAiKey ? "OPENAI" : (hasGeminiKey ? "GEMINI" : "NONE")));
            status.put("groqConfigured", hasGroqKey);
            status.put("openaiConfigured", hasOpenAiKey);
            status.put("geminiConfigured", hasGeminiKey);
        }
        status.put("groqModel", groqModel);
        if (rateLimitTracker != null) {
            status.put("rateLimits", rateLimitTracker.getRateLimitSnapshot());
        }
        if (concurrencyLimiter != null) {
            status.put("activeInFlight", concurrencyLimiter.getInFlightCount());
            status.put("concurrencyLimit", concurrencyLimiter.getConcurrencyLimit());
            status.put("queuedRequests", concurrencyLimiter.getQueuedCount());
            status.put("queueCapacity", concurrencyLimiter.getQueueCapacity());
        }
        return status;
    }

    public Map<String, Object> getAdminUsageDashboard() {
        if (usageMetricsService != null) {
            return usageMetricsService.getAdminDashboard();
        }
        return Collections.singletonMap("status", "Usage metrics service not initialized");
    }

    private ChatSession resolveSession(ChatSession session, String email, User user, String contextType) {
        if (session != null && session.getId() != null) {
            Optional<ChatSession> opt = chatSessionRepository.findById(session.getId());
            if (opt.isPresent()) return opt.get();
        }
        ChatSession newSession = new ChatSession();
        newSession.setUserEmail(email);
        newSession.setUser(user);
        newSession.setContextType(contextType);
        newSession.setTitle("New Session");
        newSession.setStatus("ACTIVE");
        return chatSessionRepository.save(newSession);
    }

    private void updateSessionTitleIfNeeded(ChatSession session, String userMessage) {
        if ("New Session".equalsIgnoreCase(session.getTitle()) || "New Learning Session".equalsIgnoreCase(session.getTitle())) {
            String clean = userMessage.trim().replaceAll("[\\r\\n]+", " ");
            String[] words = clean.split("\\s+");
            String title = words.length > 5 ? words[0] + " " + words[1] + " " + words[2] + " " + words[3] + "..." : clean;
            session.setTitle(title);
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);
        }
    }

    // Inner value object for AI response
    public static class AiResponse {
        private final boolean success;
        private final String text;
        private final String modelVersion;
        private final String errorMessage;

        public AiResponse(boolean success, String text, String modelVersion, String errorMessage) {
            this.success = success;
            this.text = text;
            this.modelVersion = modelVersion;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getText() { return text; }
        public String getModelVersion() { return modelVersion; }
        public String getErrorMessage() { return errorMessage; }
    }

    // Backwards compatibility alias for GeminiResponse
    public static class GeminiResponse extends AiResponse {
        public GeminiResponse(boolean success, String text, String modelVersion, String errorMessage) {
            super(success, text, modelVersion, errorMessage);
        }
    }
}
