package com.vrsec.backend.controller;

import com.vrsec.backend.model.ChatMessage;
import com.vrsec.backend.model.ChatSession;
import com.vrsec.backend.model.User;
import com.vrsec.backend.repository.ChatMessageRepository;
import com.vrsec.backend.repository.ChatSessionRepository;
import com.vrsec.backend.repository.UserRepository;
import com.vrsec.backend.service.AiAssistantService;
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

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(@RequestParam String email) {
        try {
            String cleanEmail = email != null ? email.trim().toLowerCase() : "";
            List<ChatSession> sessions = chatSessionRepository.findByUserEmailIgnoreCaseOrderByUpdatedAtDesc(cleanEmail);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/new")
    public ResponseEntity<?> createSession(@RequestParam String email, @RequestParam(defaultValue = "STUDENT") String contextType) {
        try {
            String cleanEmail = email != null ? email.trim().toLowerCase() : "";
            User user = userRepository.findByEmail(cleanEmail).orElse(null);

            ChatSession session = new ChatSession();
            session.setUserEmail(cleanEmail);
            session.setUser(user);
            session.setContextType(contextType.toUpperCase());
            session.setTitle("New Session");
            session.setStatus("ACTIVE");
            session = chatSessionRepository.save(session);

            // Add initial welcome greeting
            String welcome = "FACULTY".equalsIgnoreCase(contextType)
                    ? "Welcome to the VRSEC Faculty AI Advisory Hub. How can I assist your department analytics today?"
                    : "Hello! I am your LearnPath AI Tutor. Ask me any question about your courses, code examples, or exam preparation!";

            chatMessageRepository.save(new ChatMessage(session, "ai", welcome));

            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable Long sessionId) {
        try {
            List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
            List<Map<String, Object>> response = new ArrayList<>();
            for (ChatMessage m : messages) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("sender", m.getSender());
                map.put("role", m.getSender()); // alias for both client styles
                map.put("content", m.getContent());
                map.put("text", m.getContent()); // alias for both client styles
                map.put("timestamp", m.getTimestamp());
                response.add(map);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/update-status/{sessionId}")
    public ResponseEntity<?> updateStatus(@PathVariable Long sessionId, @RequestParam String status) {
        try {
            Optional<ChatSession> opt = chatSessionRepository.findById(sessionId);
            if (opt.isPresent()) {
                ChatSession session = opt.get();
                session.setStatus(status.toUpperCase());
                session.setUpdatedAt(LocalDateTime.now());
                chatSessionRepository.save(session);
                return ResponseEntity.ok(Collections.singletonMap("message", "Session status updated to " + status));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> req) {
        try {
            String email = (String) req.get("email");
            String message = (String) req.get("message");
            if (message == null && req.get("content") != null) message = req.get("content").toString();
            if (email == null || message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Email and message are required."));
            }

            Long sessionId = null;
            if (req.get("sessionId") != null) {
                try { sessionId = Long.valueOf(req.get("sessionId").toString()); } catch (Exception ignored) {}
            }

            User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
            boolean isFaculty = user != null && user.isFaculty();

            Map<String, Object> result;
            if (isFaculty) {
                result = aiAssistantService.chatWithFacultyAi(email, sessionId, message);
            } else {
                result = aiAssistantService.chatWithStudentAi(email, sessionId, message);
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
