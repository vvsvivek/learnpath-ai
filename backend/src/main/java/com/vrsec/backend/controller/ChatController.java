package com.vrsec.backend.controller;

import com.vrsec.backend.service.GeminiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v2/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final GeminiChatService geminiChatService;

    public ChatController(GeminiChatService geminiChatService) {
        this.geminiChatService = geminiChatService;
    }

    @PostMapping("/new")
    public ResponseEntity<?> createNewChat(@RequestParam(required = false) String email) {
        return ResponseEntity.ok(Map.of("id", System.currentTimeMillis()));
    }

    @PostMapping("/send")
    public ResponseEntity<?> handleChatMessage(@RequestBody Map<String, Object> payload) {
        String email = (String) payload.getOrDefault("email", "");
        String message = (String) payload.getOrDefault("message", "");
        long sessionId = payload.containsKey("sessionId") && payload.get("sessionId") != null 
                ? Long.parseLong(payload.get("sessionId").toString()) 
                : System.currentTimeMillis();

        String aiReply = geminiChatService.getGenerativeAIResponse(email, message);
        
        String[] words = message.split("\\s+");
        String chatTitle = words.length > 4 ? String.join(" ", Arrays.copyOfRange(words, 0, 4)) + "..." : message;

        Map<String, Object> response = new HashMap<>();
        response.put("reply", aiReply);
        response.put("sessionId", sessionId);
        response.put("title", chatTitle);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(@RequestParam(required = false) String email) {
        return ResponseEntity.ok(new ArrayList<>()); 
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<?> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(new ArrayList<>()); 
    }

    @PostMapping("/update-status/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }
}