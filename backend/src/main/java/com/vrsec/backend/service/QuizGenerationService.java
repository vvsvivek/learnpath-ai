package com.vrsec.backend.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class QuizGenerationService {

    private final String LOCAL_AI_URL = "http://localhost:11434/api/generate"; 

    @SuppressWarnings("unchecked") 
    public String generateUniqueQuestions(String topic, String course) {
        RestTemplate restTemplate = new RestTemplate();

        String prompt = String.format(
            "You are an expert university examiner teaching '%s'. Generate exactly 5 highly unique, randomized multiple-choice questions for the sub-topic '%s'. " +
            "CRITICAL: Do not use generic questions. Focus on random edge-cases, deep concepts, and practical applications to ensure questions never repeat. " +
            "You must output ONLY a valid JSON array with absolutely no markdown formatting, no introduction, and no extra text. " +
            "Use this exact schema: " +
            "[ { \"q\": \"The question text\", \"options\": [\"Option 1\", \"Option 2\", \"Option 3\", \"Option 4\"], \"correct\": \"The exact text of the correct option\" } ]",
            course, topic
        );

        Map<String, Object> request = new HashMap<>();
        request.put("model", "llama3"); 
        request.put("prompt", prompt);
        request.put("stream", false);
        request.put("temperature", 0.8); 

        try {
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(LOCAL_AI_URL, request, Map.class);
            if (response != null && response.containsKey("response")) {
                String aiOutput = response.get("response").toString();
                return aiOutput.replace("```json", "").replace("```", "").trim();
            }
        } catch (RestClientException e) {
            System.err.println("AI Generation Error: " + e.getMessage());
        }
        
        return "[{\"q\": \"Backend AI connection failed. What is the status?\", \"options\": [\"Offline\", \"Online\", \"Rebooting\", \"Unknown\"], \"correct\": \"Offline\"}]";
    }
}