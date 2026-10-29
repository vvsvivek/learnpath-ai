package com.vrsec.backend.controller;

import com.vrsec.backend.service.GeminiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v2/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired 
    private GeminiChatService geminiChatService;

    private static final Map<Long, List<Map<String, String>>> sessionHistories = new HashMap<>();
    private static final Map<Long, Map<String, Object>> sessionDetails = new HashMap<>();

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(@RequestParam String email) {
        try {
            String cleanEmail = email != null ? email.trim().toLowerCase() : "";
            List<Map<String, Object>> userSessions = new ArrayList<>();
            
            for (Map.Entry<Long, Map<String, Object>> entry : sessionDetails.entrySet()) {
                Map<String, Object> details = entry.getValue();
                if (cleanEmail.equalsIgnoreCase((String) details.get("email"))) {
                    userSessions.add(details);
                }
            }
            userSessions.sort((a, b) -> Long.compare((Long) b.get("id"), (Long) a.get("id")));
            return ResponseEntity.ok(userSessions);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/new")
    public ResponseEntity<?> newSession(@RequestParam String email) {
        try {
            long sessionId = System.currentTimeMillis();
            Map<String, Object> meta = new HashMap<>();
            meta.put("id", sessionId);
            meta.put("email", email != null ? email.trim().toLowerCase() : "");
            meta.put("title", "New Session");
            meta.put("status", "ACTIVE");

            sessionDetails.put(sessionId, meta);
            sessionHistories.put(sessionId, new ArrayList<>());

            return ResponseEntity.ok(meta);
        } catch (Exception e) {
            long sessionId = System.currentTimeMillis();
            return ResponseEntity.ok(Map.of("id", sessionId, "title", "New Session", "status", "ACTIVE"));
        }
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable Long sessionId) {
        List<Map<String, String>> history = sessionHistories.getOrDefault(sessionId, new ArrayList<>());
        return ResponseEntity.ok(history);
    }

    @PostMapping("/update-status/{sessionId}")
    public ResponseEntity<?> updateStatus(@PathVariable Long sessionId, @RequestParam String status) {
        Map<String, Object> meta = sessionDetails.get(sessionId);
        if (meta != null) {
            meta.put("status", status.toUpperCase());
            return ResponseEntity.ok(Collections.singletonMap("message", "Status updated"));
        }
        return ResponseEntity.ok(Collections.singletonMap("message", "Status updated"));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> req) {
        Long sessionId = null;
        try {
            String email = (String) req.get("email");
            String message = (String) req.get("message");
            sessionId = req.get("sessionId") != null ? Long.valueOf(req.get("sessionId").toString()) : System.currentTimeMillis();

            if (!sessionDetails.containsKey(sessionId)) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("id", sessionId);
                meta.put("email", email != null ? email.trim().toLowerCase() : "");
                meta.put("title", "New Session");
                meta.put("status", "ACTIVE");
                sessionDetails.put(sessionId, meta);
                sessionHistories.put(sessionId, new ArrayList<>());
            }

            String aiReply = geminiChatService.getGenerativeAIResponse(email, message);
            if (aiReply == null || aiReply.trim().isEmpty()) {
                aiReply = "Hello! I am your LearnPath AI Tutor. I am ready to guide you through your courses and academic goals.";
            }

            List<Map<String, String>> history = sessionHistories.get(sessionId);
            if (history == null) {
                history = new ArrayList<>();
                sessionHistories.put(sessionId, history);
            }
            history.add(Map.of("role", "user", "content", message != null ? message : ""));
            history.add(Map.of("role", "ai", "content", aiReply));

            Map<String, Object> meta = sessionDetails.get(sessionId);
            String currentTitle = meta != null ? (String) meta.get("title") : "New Session";
            if (meta != null && ("New Session".equals(currentTitle) || currentTitle == null)) {
                String cleanMsg = message != null ? message.trim() : "Session";
                String[] words = cleanMsg.split("\\s+");
                String autoTitle = words.length > 4 ? words[0] + " " + words[1] + " " + words[2] + "..." : cleanMsg;
                meta.put("title", autoTitle);
                currentTitle = autoTitle;
            }

            Map<String, Object> res = new HashMap<>();
            res.put("sessionId", sessionId);
            res.put("reply", aiReply);
            res.put("message", aiReply);
            res.put("response", aiReply);
            res.put("title", currentTitle != null ? currentTitle : "Session");

            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> fallbackRes = new HashMap<>();
            String fallbackText = "Hello! I am your LearnPath AI Tutor. How can I assist you with your learning path today?";
            fallbackRes.put("sessionId", sessionId != null ? sessionId : System.currentTimeMillis());
            fallbackRes.put("reply", fallbackText);
            fallbackRes.put("message", fallbackText);
            fallbackRes.put("response", fallbackText);
            fallbackRes.put("title", "Active Session");
            return ResponseEntity.ok(fallbackRes);
        }
    }
}