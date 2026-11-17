package com.vrsec.backend.controller;

import com.vrsec.backend.model.ChatMessage;
import com.vrsec.backend.model.ChatSession;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.ChatMessageRepository;
import com.vrsec.backend.repository.ChatSessionRepository;
import com.vrsec.backend.repository.UserRepository;
import com.vrsec.backend.service.AiAssistantService;
import com.vrsec.backend.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping({"/v2/chat", "/api/main/chat", "/api/chat"})
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired private AiAssistantService aiAssistantService;
    @Autowired private ChatSessionRepository chatSessionRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    /**
     * Extracts and validates the authenticated email strictly from the Bearer JWT token.
     * Never trusts request body or query parameters for identity.
     */
    private String getAuthenticatedEmail(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        Map<String, Object> claims = jwtService.validateAndExtractClaims(token);
        if (claims == null || claims.get("sub") == null) {
            return null;
        }
        return ((String) claims.get("sub")).trim().toLowerCase();
    }

    /**
     * Retrieve chat sessions owned exclusively by the authenticated user.
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String email) {
        try {
            String authEmail = getAuthenticatedEmail(authHeader);
            if (authEmail == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Bearer token."));
            }
            List<ChatSession> sessions = chatSessionRepository.findByUserEmailIgnoreCaseOrderByUpdatedAtDesc(authEmail).stream()
                    .filter(s -> !"DELETED".equalsIgnoreCase(s.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Create a new chat session for the authenticated user with server-verified role.
     */
    @PostMapping("/new")
    public ResponseEntity<?> createSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String contextType) {
        try {
            String authEmail = getAuthenticatedEmail(authHeader);
            if (authEmail == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Bearer token."));
            }

            User user = userRepository.findByEmail(authEmail).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error", "Authenticated user not found."));
            }

            boolean isFaculty = user.isFaculty();
            String effectiveContext = isFaculty ? "FACULTY" : "STUDENT";

            ChatSession session = new ChatSession();
            session.setUserEmail(authEmail);
            session.setUser(user);
            session.setContextType(effectiveContext);
            session.setTitle("New Session");
            session.setStatus("ACTIVE");
            session = chatSessionRepository.save(session);

            // Add server-controlled initial welcome greeting based on verified role
            String welcome = isFaculty
                    ? "Welcome to the LearnPath Faculty AI Advisor. I analyze cohort analytics, remedial support requirements, and course evaluation metrics for your authorized department. How can I assist you today?"
                    : "Hello " + (user.getUsername() != null ? user.getUsername() : "there") + "! 👋 I am your LearnPath AI Tutor. Ask me any conceptual, programming, or general academic question, or ask about your enrolled courses, quizzes, and learning progress!";

            chatMessageRepository.save(new ChatMessage(session, "ai", welcome));

            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Retrieve conversation history strictly for sessions owned by the authenticated user.
     * Prevents cross-student or cross-user history access.
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long sessionId) {
        try {
            String authEmail = getAuthenticatedEmail(authHeader);
            if (authEmail == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Bearer token."));
            }

            Optional<ChatSession> opt = chatSessionRepository.findById(sessionId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", "Chat session not found."));
            }

            ChatSession session = opt.get();
            if (!authEmail.equalsIgnoreCase(session.getUserEmail())) {
                return ResponseEntity.status(403).body(Collections.singletonMap("error",
                        "Access denied: You are not authorized to view this session's history."));
            }

            List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
            List<Map<String, Object>> response = new ArrayList<>();
            for (ChatMessage m : messages) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("sender", m.getSender());
                map.put("role", m.getSender());
                map.put("content", m.getContent());
                map.put("text", m.getContent());
                map.put("timestamp", m.getTimestamp());
                response.add(map);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Update session status with verified session ownership.
     * Supports status passed either as query parameter (?status=ARCHIVED) or JSON request body ({"status":"ARCHIVED"}).
     */
    @PostMapping("/update-status/{sessionId}")
    public ResponseEntity<?> updateStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long sessionId,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String authEmail = getAuthenticatedEmail(authHeader);
            if (authEmail == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Bearer token."));
            }

            String effectiveStatus = status;
            if ((effectiveStatus == null || effectiveStatus.trim().isEmpty()) && body != null && body.containsKey("status")) {
                effectiveStatus = String.valueOf(body.get("status"));
            }
            if (effectiveStatus == null || effectiveStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Status parameter is required."));
            }

            Optional<ChatSession> opt = chatSessionRepository.findById(sessionId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", "Chat session not found."));
            }

            ChatSession session = opt.get();
            if (!authEmail.equalsIgnoreCase(session.getUserEmail())) {
                return ResponseEntity.status(403).body(Collections.singletonMap("error",
                        "Access denied: You are not authorized to modify this session."));
            }

            session.setStatus(effectiveStatus.trim().toUpperCase());
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);
            return ResponseEntity.ok(Collections.singletonMap("message", "Session status updated to " + effectiveStatus.trim().toUpperCase()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Reports backend AI engine configuration and active model status.
     * Enforces JWT authentication. Never exposes keys.
     */
    @GetMapping({"/ai-status", "/status"})
    public ResponseEntity<?> getAiStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String authEmail = getAuthenticatedEmail(authHeader);
            if (authEmail == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Bearer token."));
            }
            Map<String, Object> status = aiAssistantService.getAiEngineStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Admin Usage Dashboard: Returns real-time Groq TPM/RPM rate-limit status,
     * in-flight concurrency, queue metrics, total token consumption, and per-user metrics.
     * Strictly restricted to verified ADMIN accounts.
     */
    @GetMapping({"/admin/usage", "/admin/metrics"})
    public ResponseEntity<?> getAdminUsage(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String authEmail = getAuthenticatedEmail(authHeader);
            if (authEmail == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Bearer token."));
            }

            User user = userRepository.findByEmail(authEmail).orElse(null);
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(403).body(Collections.singletonMap("error",
                        "Access denied: Administrator privileges required to view AI usage metrics."));
            }

            Map<String, Object> dashboard = aiAssistantService.getAdminUsageDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Delete (archive) a chat session with verified session ownership.
     */
    @DeleteMapping({"/session/{sessionId}", "/sessions/{sessionId}", "/{sessionId}"})
    public ResponseEntity<?> deleteSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long sessionId) {
        try {
            String authEmail = getAuthenticatedEmail(authHeader);
            if (authEmail == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Bearer token."));
            }

            Optional<ChatSession> opt = chatSessionRepository.findById(sessionId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", "Chat session not found."));
            }

            ChatSession session = opt.get();
            if (!authEmail.equalsIgnoreCase(session.getUserEmail())) {
                return ResponseEntity.status(403).body(Collections.singletonMap("error",
                        "Access denied: You are not authorized to delete this session."));
            }

            session.setStatus("DELETED");
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);
            return ResponseEntity.ok(Collections.singletonMap("message", "Session successfully deleted."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Send a message to AI.
     * Enforces JWT authentication, session ownership, server-side role retrieval,
     * and returns genuine AI responses or transparent retryable errors.
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> req) {
        try {
            String authEmail = getAuthenticatedEmail(authHeader);
            if (authEmail == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error",
                        "Authentication required: Missing or invalid Bearer token."));
            }

            String message = (String) req.get("message");
            if (message == null && req.get("content") != null) message = req.get("content").toString();
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Message cannot be empty."));
            }

            User user = userRepository.findByEmail(authEmail).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body(Collections.singletonMap("error", "Authenticated user not found."));
            }

            Long sessionId = null;
            if (req.get("sessionId") != null) {
                try { sessionId = Long.valueOf(req.get("sessionId").toString()); } catch (Exception ignored) {}
            }

            ChatSession session = null;
            if (sessionId != null) {
                Optional<ChatSession> opt = chatSessionRepository.findById(sessionId);
                if (opt.isEmpty()) {
                    return ResponseEntity.status(404).body(Collections.singletonMap("error", "Chat session not found."));
                }
                session = opt.get();
                if (!authEmail.equalsIgnoreCase(session.getUserEmail())) {
                    return ResponseEntity.status(403).body(Collections.singletonMap("error",
                            "Access denied: You are not authorized to send messages to this session."));
                }
            }

            boolean isFaculty = user.isFaculty();
            Map<String, Object> result;
            if (isFaculty) {
                result = aiAssistantService.chatWithFacultyAi(user, session, message.trim());
            } else {
                result = aiAssistantService.chatWithStudentAi(user, session, message.trim());
            }

            if (Boolean.TRUE.equals(result.get("isError"))) {
                return ResponseEntity.status(503).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", "AI error: " + e.getMessage()));
        }
    }
}
