package com.vrsec.backend.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * OpenAI LLM Provider abstraction implementation.
 */
@Service
public class OpenAiProvider implements AiProvider {

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel = "gpt-4o-mini";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public boolean isConfigured() {
        return openaiApiKey != null && !openaiApiKey.trim().isEmpty() && !openaiApiKey.contains("YOUR_");
    }

    @Override
    public AiProviderResponse executeChat(AiProviderRequest request) {
        if (!isConfigured()) {
            return AiProviderResponse.failure("OpenAI API key is not configured", openaiModel, 503, 0, Collections.emptyMap());
        }

        String effectiveModel = (request.getTargetModel() != null && !request.getTargetModel().trim().isEmpty())
                ? request.getTargetModel().trim()
                : openaiModel;
        String apiUrl = "https://api.openai.com/v1/responses";
        long startTime = System.currentTimeMillis();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey.trim());

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", effectiveModel);
            if (request.getSystemPromptPrefix() != null && !request.getSystemPromptPrefix().trim().isEmpty()) {
                payload.put("instructions", request.getSystemPromptPrefix().trim());
            }
            payload.put("input", request.getMessages());

            HttpEntity<String> httpEntity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(apiUrl, httpEntity, Map.class);
            long latency = System.currentTimeMillis() - startTime;

            Map body = responseEntity.getBody();
            String extracted = extractOpenAiText(body);
            String modelVersion = (body != null && body.containsKey("model"))
                    ? String.valueOf(body.get("model"))
                    : effectiveModel;

            if (extracted != null && !extracted.trim().isEmpty()) {
                return AiProviderResponse.success(extracted.trim(), modelVersion, 0, 0, 0, latency, Collections.emptyMap());
            }
            return AiProviderResponse.failure("Empty response received from OpenAI (" + effectiveModel + ")",
                    effectiveModel, 502, latency, Collections.emptyMap());

        } catch (HttpStatusCodeException e) {
            long latency = System.currentTimeMillis() - startTime;
            String errorMsg = "HTTP " + e.getStatusCode().value() + " " + e.getStatusText();
            try {
                String body = e.getResponseBodyAsString();
                if (body != null && !body.trim().isEmpty()) {
                    Map err = objectMapper.readValue(body, Map.class);
                    if (err != null && err.containsKey("error") && err.get("error") instanceof Map) {
                        Map<?, ?> errMap = (Map<?, ?>) err.get("error");
                        if (errMap.containsKey("message") && errMap.get("message") != null) {
                            errorMsg = String.valueOf(errMap.get("message"));
                        }
                    }
                }
            } catch (Exception ignored) {}
            return AiProviderResponse.failure(errorMsg, effectiveModel, e.getStatusCode().value(), latency, Collections.emptyMap());
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return AiProviderResponse.failure(e.getMessage(), effectiveModel, 500, latency, Collections.emptyMap());
        }
    }

    @Override
    public AiProviderResponse testConnection(String model) {
        if (!isConfigured()) {
            return AiProviderResponse.failure("OpenAI API key is not configured", openaiModel, 503, 0, Collections.emptyMap());
        }
        AiProviderRequest probe = new AiProviderRequest(
                null,
                null,
                Collections.singletonList(Map.of("role", "user", "content", "Reply with exactly: OpenAI connection successful.")),
                model != null ? model : openaiModel,
                50,
                0.0,
                "health-probe"
        );
        return executeChat(probe);
    }

    private String extractOpenAiText(Map response) {
        if (response == null) return null;
        if (response.containsKey("output_text") && response.get("output_text") != null) {
            String t = String.valueOf(response.get("output_text")).trim();
            if (!t.isEmpty()) return t;
        }
        if (response.containsKey("choices") && response.get("choices") instanceof List) {
            List<?> choices = (List<?>) response.get("choices");
            if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                Map<?, ?> first = (Map<?, ?>) choices.get(0);
                if (first.containsKey("message") && first.get("message") instanceof Map) {
                    Map<?, ?> msg = (Map<?, ?>) first.get("message");
                    if (msg.containsKey("content") && msg.get("content") != null) {
                        return String.valueOf(msg.get("content")).trim();
                    }
                }
            }
        }
        return null;
    }
}
