package com.vrsec.backend.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Google Gemini LLM Provider abstraction implementation.
 */
@Service
public class GeminiAiProvider implements AiProvider {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public boolean isConfigured() {
        return geminiApiKey != null && !geminiApiKey.trim().isEmpty() && !geminiApiKey.contains("YOUR_");
    }

    @Override
    public AiProviderResponse executeChat(AiProviderRequest request) {
        if (!isConfigured()) {
            return AiProviderResponse.failure("Gemini API key is not configured", "gemini-3.8-flash", 503, 0, Collections.emptyMap());
        }

        // Map messages to Gemini parts format
        List<Map<String, Object>> geminiContents = new ArrayList<>();
        if (request.getMessages() != null) {
            for (Map<String, Object> c : request.getMessages()) {
                String role = "assistant".equals(c.get("role")) ? "model" : "user";
                String text = String.valueOf(c.get("content"));
                geminiContents.add(Map.of("role", role, "parts", Collections.singletonList(Map.of("text", text))));
            }
        }

        String[] candidateModels = {"gemini-3.8-flash", "gemini-3.5-flash-lite"};
        long startTime = System.currentTimeMillis();
        String lastError = null;

        for (String model : candidateModels) {
            try {
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("x-goog-api-key", geminiApiKey.trim());

                Map<String, Object> payload = new HashMap<>();
                if (request.getSystemPromptPrefix() != null && !request.getSystemPromptPrefix().trim().isEmpty()) {
                    payload.put("systemInstruction", Map.of(
                            "parts", Collections.singletonList(Map.of("text", request.getSystemPromptPrefix().trim()))
                    ));
                }
                payload.put("contents", geminiContents);
                payload.put("generationConfig", Map.of("temperature", request.getTemperature(), "maxOutputTokens", 2048));

                HttpEntity<String> httpEntity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity(apiUrl, httpEntity, Map.class);
                long latency = System.currentTimeMillis() - startTime;

                Map body = responseEntity.getBody();
                if (body != null && body.containsKey("candidates")) {
                    List candidates = (List) body.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map first = (Map) candidates.get(0);
                        Map content = (Map) first.get("content");
                        if (content != null && content.containsKey("parts")) {
                            List parts = (List) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                String text = (String) ((Map) parts.get(0)).get("text");
                                if (text != null && !text.trim().isEmpty()) {
                                    String modelVersion = body.containsKey("modelVersion") ?
                                            String.valueOf(body.get("modelVersion")) : model;
                                    return AiProviderResponse.success(text.trim(), modelVersion, 0, 0, 0, latency, Collections.emptyMap());
                                }
                            }
                        }
                    }
                }
                lastError = "Empty response received from " + model;
            } catch (Exception e) {
                lastError = model + " failed: " + e.getMessage();
            }
        }

        long latency = System.currentTimeMillis() - startTime;
        return AiProviderResponse.failure(lastError != null ? lastError : "Gemini API invocation failed",
                "gemini-3.8-flash", 500, latency, Collections.emptyMap());
    }

    @Override
    public AiProviderResponse testConnection(String model) {
        if (!isConfigured()) {
            return AiProviderResponse.failure("Gemini API key is not configured", "gemini-3.8-flash", 503, 0, Collections.emptyMap());
        }
        AiProviderRequest probe = new AiProviderRequest(
                null,
                null,
                Collections.singletonList(Map.of("role", "user", "content", "ping")),
                "gemini-3.8-flash",
                50,
                0.0,
                "health-probe"
        );
        return executeChat(probe);
    }
}
