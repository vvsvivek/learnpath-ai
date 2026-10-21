package com.vrsec.backend.service;

import com.vrsec.backend.model.Course;
import com.vrsec.backend.model.QuizAttempt;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.CourseRepository;
import com.vrsec.backend.repository.QuizAttemptRepository;
import com.vrsec.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeminiChatService {

    // IMPORTANT: Add GEMINI_API_KEY to your application.properties or .env
    @Value("${gemini.api.key:YOUR_API_KEY_HERE}")
    private String geminiApiKey;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiChatService(UserRepository userRepository, CourseRepository courseRepository, QuizAttemptRepository quizAttemptRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public String getGenerativeAIResponse(String email, String userMessage) {
        String studentContext = buildSecureStudentContext(email);
        
        // System prompt instructing Gemini how to act
        String systemPrompt = "You are the 'LearnPath AI Tutor', an expert academic assistant for B.Tech students. " +
                "You are professional, concise, and format your answers beautifully using Markdown (bolding, bullet points). " +
                "Below is the verified, real-time database record of the student you are talking to. " +
                "If they ask about their progress, courses, or quiz scores, ONLY use this data. " +
                "Do NOT invent data. If they ask a general academic question (like 'what is python'), answer it comprehensively.\n\n" +
                "=== SECURE STUDENT DATABASE CONTEXT ===\n" + studentContext + "\n=======================================\n\n" +
                "Student's Question: " + userMessage;

        return callGoogleGeminiApi(systemPrompt);
    }

    private String buildSecureStudentContext(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return "User not authenticated.";

        User user = userOpt.get();
        StringBuilder context = new StringBuilder();
        context.append("Name: ").append(user.getUsername()).append("\n");
        context.append("Branch: ").append(user.getBranch()).append(" (").append(user.getAcademicYear()).append(", ").append(user.getSemester()).append(")\n");
        context.append("Current CGPA: ").append(user.getCgpa()).append("\n\n");

        List<Course> activeCourses = courseRepository.findByUserEmailAndDeletedFalse(email);
        context.append("Enrolled Courses:\n");
        if (activeCourses.isEmpty()) {
            context.append("- None currently enrolled.\n");
        } else {
            for (Course c : activeCourses) {
                context.append("- ").append(c.getTitle()).append(" (Progress: ").append(c.getProgress() != null ? c.getProgress() : 0).append("%)\n");
            }
        }

        List<QuizAttempt> quizzes = quizAttemptRepository.findByUser(user);
        context.append("\nRecent Quiz Results:\n");
        if (quizzes.isEmpty()) {
            context.append("- No quizzes attempted yet.\n");
        } else {
            for (QuizAttempt q : quizzes) {
                context.append("- Topic: ").append(q.getTopic()).append(" | Score: ").append(q.getScore()).append("/").append(q.getTotalQuestions()).append("\n");
            }
        }

        return context.toString();
    }

    private String callGoogleGeminiApi(String prompt) {
        try {
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Constructing the exact JSON body Gemini expects
            String requestBody = "{\n" +
                    "  \"contents\": [{\n" +
                    "    \"parts\":[{\"text\": \"" + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]\n" +
                    "  }]\n" +
                    "}";

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            Map response = restTemplate.postForObject(apiUrl, request, Map.class);

            // Parsing the Gemini response safely
            if (response != null && response.containsKey("candidates")) {
                List candidates = (List) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map firstCandidate = (Map) candidates.get(0);
                    Map content = (Map) firstCandidate.get("content");
                    List parts = (List) content.get("parts");
                    Map firstPart = (Map) parts.get(0);
                    return (String) firstPart.get("text");
                }
            }
            return "I'm sorry, I couldn't generate a response at this time.";
        } catch (Exception e) {
            System.err.println("Gemini API Error: " + e.getMessage());
            return "Network Error: Could not reach the AI Knowledge Graph. Please ensure API keys are configured.";
        }
    }
}