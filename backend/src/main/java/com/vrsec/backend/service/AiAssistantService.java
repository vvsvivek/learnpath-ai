package com.vrsec.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vrsec.backend.model.*;
import com.vrsec.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CoursePhaseRepository phaseRepository;
    @Autowired private QuizAttemptRepository quizAttemptRepository;
    @Autowired private TopicPerformanceRepository topicPerformanceRepository;
    @Autowired private ChatSessionRepository chatSessionRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // 1. STUDENT AI ASSISTANT (Individual Context-Aware AI Mentor)
    // =========================================================================
    @Transactional
    public Map<String, Object> chatWithStudentAi(String email, Long sessionId, String userMessage) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        User student = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        ChatSession session = resolveSession(sessionId, cleanEmail, student, "STUDENT");

        // Save incoming user message in DB
        ChatMessage userMsg = new ChatMessage(session, "user", userMessage);
        chatMessageRepository.save(userMsg);

        // Fetch student's real database context
        List<Course> activeCourses = courseRepository.findByUserEmailIgnoreCaseAndDeletedFalse(cleanEmail);
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdOrderByCreatedAtDesc(student.getId());
        List<TopicPerformance> weakTopics = topicPerformanceRepository.findByUserIdAndStatus(student.getId(), "NEEDS_ATTENTION");

        String contextSummary = buildStudentContext(student, activeCourses, attempts, weakTopics);

        String systemPrompt = "You are 'LearnPath AI Student Mentor', a deeply encouraging, expert academic tutor.\n"
                + "Formatting rules: Use clear GitHub-flavored markdown. Use bolding for emphasis, bulleted lists for steps, and syntax-highlighted code blocks for programming snippets.\n"
                + "You have full access to this student's verified, real-time university database records below.\n"
                + "When they ask about their courses, progress, scores, or weak topics, answer STRICTLY using these real records.\n"
                + "If they ask to explain concepts, give intuitive analogies followed by practical code or real-world engineering examples.\n\n"
                + "=== AUTHENTICATED STUDENT CONTEXT ===\n" + contextSummary + "\n====================================\n\n"
                + "Student Question: " + userMessage;

        String aiReply = callGemini(systemPrompt);
        if (aiReply == null || aiReply.trim().isEmpty()) {
            aiReply = generateFallbackStudentResponse(student, activeCourses, attempts, weakTopics, userMessage);
        }

        // Save AI reply in DB
        ChatMessage aiMsg = new ChatMessage(session, "ai", aiReply);
        chatMessageRepository.save(aiMsg);

        // Auto-update session title if it's default
        if ("New Session".equalsIgnoreCase(session.getTitle()) || "New Learning Session".equalsIgnoreCase(session.getTitle())) {
            String[] words = userMessage.trim().split("\\s+");
            String title = words.length > 5 ? words[0] + " " + words[1] + " " + words[2] + " " + words[3] + "..." : userMessage.trim();
            session.setTitle(title);
            chatSessionRepository.save(session);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("title", session.getTitle());
        response.put("reply", aiReply);
        response.put("message", aiReply);
        return response;
    }

    private String buildStudentContext(User student, List<Course> courses, List<QuizAttempt> attempts, List<TopicPerformance> weakTopics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Student Name: ").append(student.getUsername()).append("\n");
        sb.append("Branch: ").append(student.getBranch()).append(" | Year: ").append(student.getAcademicYear()).append(" | Sem: ").append(student.getSemester()).append("\n");
        sb.append("Current CGPA: ").append(student.getCgpa()).append("\n");

        sb.append("\nEnrolled Courses (").append(courses.size()).append("):\n");
        for (Course c : courses) {
            sb.append("- ").append(c.getTitle()).append(" (Progress: ").append(c.getProgress()).append("%, Current Phase: ").append(c.getCurrentPhase()).append("/").append(c.getTotalPhases()).append(")\n");
        }

        sb.append("\nRecent Quiz Attempts (").append(attempts.size()).append("):\n");
        for (QuizAttempt a : attempts.stream().limit(5).collect(Collectors.toList())) {
            sb.append("- ").append(a.getTopic()).append(": Score ").append(a.getScore()).append("/").append(a.getTotalQuestions())
              .append(" (").append(a.getPercentage()).append("% - ").append(a.getPassed() ? "PASSED" : "FAILED").append(")\n");
        }

        sb.append("\nIdentified Weak Topics Needing Attention:\n");
        if (weakTopics.isEmpty()) {
            sb.append("None currently detected. Strong performance!\n");
        } else {
            for (TopicPerformance tp : weakTopics) {
                sb.append("- ").append(tp.getCourseTitle()).append(" -> ").append(tp.getConceptTag()).append(" (Tested: ").append(tp.getTimesTested()).append(", Failed: ").append(tp.getTimesIncorrect()).append(")\n");
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // 2. FACULTY AI ASSISTANT (Institutional Analytics AI Engine)
    // =========================================================================
    @Transactional
    public Map<String, Object> chatWithFacultyAi(String facultyEmail, Long sessionId, String facultyMessage) {
        String cleanEmail = facultyEmail != null ? facultyEmail.trim().toLowerCase() : "";
        User faculty = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Faculty member not found"));

        if (!faculty.isFaculty()) {
            throw new SecurityException("Unauthorized: Only verified faculty accounts may access the Faculty AI Hub.");
        }

        ChatSession session = resolveSession(sessionId, cleanEmail, faculty, "FACULTY");

        // Save incoming faculty query
        chatMessageRepository.save(new ChatMessage(session, "user", facultyMessage));

        // Fetch verified @vrsec.ac.in eligible students strictly
        List<User> eligibleStudents = userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()))
                .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().endsWith("@vrsec.ac.in"))
                .collect(Collectors.toList());

        List<QuizAttempt> allAttempts = quizAttemptRepository.findAll();
        List<Course> allCourses = courseRepository.findAll();

        String analyticsData = buildFacultyAnalyticsContext(eligibleStudents, allAttempts, allCourses);

        String systemPrompt = "You are 'LearnPath AI Faculty Advisory Engine', an executive academic intelligence system for VRSEC professors.\n"
                + "CRITICAL SECURITY RULE: You ONLY analyze verified '@vrsec.ac.in' students. Faculty accounts must NEVER be counted as students.\n"
                + "Never invent statistics. Use the real database counts and metrics provided below.\n"
                + "Provide clear, structured, respectful academic insights with metrics, trends, branch-wise breakdowns, and actionable teaching interventions.\n\n"
                + "=== VRSEC REAL-TIME DATABASE ANALYTICS ===\n" + analyticsData + "\n=========================================\n\n"
                + "Faculty Query: " + facultyMessage;

        String aiReply = callGemini(systemPrompt);
        if (aiReply == null || aiReply.trim().isEmpty()) {
            aiReply = generateFallbackFacultyResponse(eligibleStudents, allAttempts, facultyMessage);
        }

        chatMessageRepository.save(new ChatMessage(session, "ai", aiReply));

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("title", session.getTitle());
        response.put("reply", aiReply);
        response.put("message", aiReply);
        return response;
    }

    private String buildFacultyAnalyticsContext(List<User> students, List<QuizAttempt> attempts, List<Course> courses) {
        StringBuilder sb = new StringBuilder();
        sb.append("Total Eligible @vrsec.ac.in Students: ").append(students.size()).append("\n");

        Map<String, Long> branchCount = students.stream()
                .collect(Collectors.groupingBy(u -> u.getBranch() != null ? u.getBranch().toUpperCase() : "UNKNOWN", Collectors.counting()));
        sb.append("Branch Distribution: ").append(branchCount).append("\n");

        Map<String, Long> yearCount = students.stream()
                .collect(Collectors.groupingBy(u -> u.getAcademicYear() != null ? u.getAcademicYear() : "UNKNOWN", Collectors.counting()));
        sb.append("Year Distribution: ").append(yearCount).append("\n");

        long activeLearners = courses.stream().filter(c -> !c.isDeleted() && c.getUserEmail().endsWith("@vrsec.ac.in")).count();
        long completedCourses = courses.stream().filter(c -> c.getProgress() != null && c.getProgress() == 100 && c.getUserEmail().endsWith("@vrsec.ac.in")).count();
        sb.append("Total Active Enrolled Courses: ").append(activeLearners).append("\n");
        sb.append("Total Completed Courses: ").append(completedCourses).append("\n");

        // Identify students requiring additional support (CGPA < 6.5 or failed quizzes)
        List<User> needsSupport = students.stream().filter(u -> u.getCgpa() != null && u.getCgpa() > 0 && u.getCgpa() < 6.5).collect(Collectors.toList());
        sb.append("Students Requiring Additional Support (CGPA < 6.5): ").append(needsSupport.size()).append("\n");
        for (User u : needsSupport) {
            sb.append("- ").append(u.getUsername()).append(" (").append(u.getEmail()).append(") | Branch: ").append(u.getBranch())
              .append(" | CGPA: ").append(u.getCgpa()).append("\n");
        }

        return sb.toString();
    }

    private ChatSession resolveSession(Long sessionId, String email, User user, String contextType) {
        if (sessionId != null) {
            Optional<ChatSession> opt = chatSessionRepository.findById(sessionId);
            if (opt.isPresent()) return opt.get();
        }
        ChatSession session = new ChatSession();
        session.setUserEmail(email);
        session.setUser(user);
        session.setContextType(contextType);
        session.setTitle("New Session");
        session.setStatus("ACTIVE");
        return chatSessionRepository.save(session);
    }

    private String callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.contains("YOUR_")) return null;
        String[] models = {"gemini-2.5-flash", "gemini-2.0-flash"};
        for (String model : models) {
            try {
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey.trim();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> payload = Map.of("contents", Collections.singletonList(
                    Map.of("parts", Collections.singletonList(Map.of("text", prompt)))
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
                                return ((String) ((Map) parts.get(0)).get("text")).trim();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Gemini Assistant call error with " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    private String generateFallbackStudentResponse(User student, List<Course> courses, List<QuizAttempt> attempts, List<TopicPerformance> weakTopics, String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello **").append(student.getUsername()).append("**! 👋\n\n");
        sb.append("Here is your real-time learning summary:\n");
        if (courses.isEmpty()) {
            sb.append("- You haven't enrolled in any courses yet. Go to **Explore Courses** or accept your **Recommended Courses** to get started!\n");
        } else {
            Course current = courses.get(0);
            sb.append("- **Current Active Subject**: ").append(current.getTitle()).append("\n");
            sb.append("- **Learning Progress**: ").append(current.getProgress()).append("% complete (Phase ").append(current.getCurrentPhase()).append(" of ").append(current.getTotalPhases()).append(")\n");
        }
        if (!weakTopics.isEmpty()) {
            sb.append("\n💡 **Focus Area Recommended**:\n");
            sb.append("Recent quiz metrics indicate you should review **").append(weakTopics.get(0).getConceptTag()).append("** in ").append(weakTopics.get(0).getCourseTitle()).append(". Would you like practice exercises on this topic?\n");
        }
        sb.append("\nFeel free to ask me to explain any concept, write sample code, or generate a tailored study plan!");
        return sb.toString();
    }

    private String generateFallbackFacultyResponse(List<User> students, List<QuizAttempt> attempts, String facultyMessage) {
        return "### VRSEC Department Academic Summary\n\n"
                + "- **Verified Eligible Students**: " + students.size() + " active @vrsec.ac.in learners\n"
                + "- **Active Courses Tracked**: " + attempts.size() + " quiz evaluations on record\n\n"
                + "Students are actively progressing through their AI learning paths. Use the filters on the **Faculty Dashboard** to inspect specific cohorts and identify learners requiring additional academic support.";
    }
}
