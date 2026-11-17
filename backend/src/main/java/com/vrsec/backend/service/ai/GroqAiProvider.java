package com.vrsec.backend.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Production-quality Groq LLM Provider.
 * Enforces bounded concurrency, exponential backoff with jitter on 429,
 * header tracking, and fair per-user sharing on the 8000 TPM limit.
 */
@Service
public class GroqAiProvider implements AiProvider {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1}")
    private String groqApiUrl = "https://api.groq.com/openai/v1";

    @Value("${groq.model:openai/gpt-oss-120b}")
    private String groqModel = "openai/gpt-oss-120b";

    @Value("${groq.max-completion-tokens:1024}")
    private int defaultMaxCompletionTokens = 1024;

    @Value("${groq.max-retries:3}")
    private int maxRetries = 3;

    @Value("${groq.base-backoff-ms:1000}")
    private long baseBackoffMs = 1000;

    @Value("${groq.max-backoff-ms:8000}")
    private long maxBackoffMs = 8000;

    @Value("${groq.user-rpm-limit:5}")
    private int userRpmLimit = 5;

    @Autowired private AiRateLimitTracker rateLimitTracker;
    @Autowired private AiConcurrencyLimiter concurrencyLimiter;
    @Autowired private AiUsageMetricsService usageMetricsService;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqAiProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10s connect timeout
        factory.setReadTimeout(30000);    // 30s read timeout
        this.restTemplate = new RestTemplate(factory);
    }

    // Constructor for testing with dependency injection
    public GroqAiProvider(
            String apiKey,
            String apiUrl,
            String model,
            AiRateLimitTracker rateLimitTracker,
            AiConcurrencyLimiter concurrencyLimiter,
            AiUsageMetricsService usageMetricsService) {
        this();
        this.groqApiKey = apiKey;
        this.groqApiUrl = apiUrl;
        this.groqModel = model;
        this.rateLimitTracker = rateLimitTracker;
        this.concurrencyLimiter = concurrencyLimiter;
        this.usageMetricsService = usageMetricsService;
    }

    @Override
    public String getProviderName() {
        return "GROQ";
    }

    @Override
    public boolean isConfigured() {
        return groqApiKey != null && !groqApiKey.trim().isEmpty() && !groqApiKey.contains("YOUR_");
    }

    @Override
    public AiProviderResponse executeChat(AiProviderRequest request) {
        if (!isConfigured()) {
            return AiProviderResponse.failure("Groq API key is not configured in backend environment",
                    groqModel, 503, 0, Collections.emptyMap());
        }

        String userIdentifier = request.getUserIdentifier();

        // 1. Fair sharing check: prevent individual users from starving the 8000 TPM limit
        if (rateLimitTracker != null) {
            String userError = rateLimitTracker.tryAcquireUserSlot(userIdentifier, userRpmLimit);
            if (userError != null) {
                if (usageMetricsService != null) {
                    usageMetricsService.recordRequest(userIdentifier, false, true, 0, 0, 0);
                }
                return AiProviderResponse.failure(userError, groqModel, 429, 0, Collections.emptyMap());
            }
        }

        long startTime = System.currentTimeMillis();
        boolean slotAcquired = false;

        try {
            // 2. Concurrency limiting & queue management
            if (concurrencyLimiter != null) {
                concurrencyLimiter.acquireSlot();
                slotAcquired = true;
            }

            return executeWithBackoff(request, startTime);

        } catch (AiConcurrencyLimiter.AiServiceOverloadedException e) {
            long latency = System.currentTimeMillis() - startTime;
            if (usageMetricsService != null) {
                usageMetricsService.recordRequest(userIdentifier, false, true, 0, 0, latency);
            }
            return AiProviderResponse.failure(e.getMessage(), groqModel, 429, latency, Collections.emptyMap());
        } catch (AiConcurrencyLimiter.AiServiceTimeoutException e) {
            long latency = System.currentTimeMillis() - startTime;
            if (usageMetricsService != null) {
                usageMetricsService.recordRequest(userIdentifier, false, false, 0, 0, latency);
            }
            return AiProviderResponse.failure(e.getMessage(), groqModel, 504, latency, Collections.emptyMap());
        } finally {
            if (slotAcquired && concurrencyLimiter != null) {
                concurrencyLimiter.releaseSlot();
            }
            if (rateLimitTracker != null) {
                rateLimitTracker.releaseUserSlot(userIdentifier);
            }
        }
    }

    private AiProviderResponse executeWithBackoff(AiProviderRequest request, long overallStartTime) {
        String effectiveModel = (request.getTargetModel() != null && !request.getTargetModel().trim().isEmpty())
                ? request.getTargetModel().trim()
                : ((groqModel != null && !groqModel.trim().isEmpty()) ? groqModel.trim() : "openai/gpt-oss-120b");

        String chatUrl = resolveChatUrl();
        int maxTokens = request.getMaxCompletionTokens() > 0 ? request.getMaxCompletionTokens() : defaultMaxCompletionTokens;

        // Construct messages array with stable system prompt prefix to maximize Groq prompt caching
        List<Map<String, Object>> messages = new ArrayList<>();
        if (request.getSystemPromptPrefix() != null && !request.getSystemPromptPrefix().trim().isEmpty()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPromptPrefix().trim()));
        }
        if (request.getMessages() != null) {
            messages.addAll(request.getMessages());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", effectiveModel);
        payload.put("messages", messages);
        payload.put("temperature", request.getTemperature());
        payload.put("max_tokens", maxTokens);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey.trim());

        int attempts = 0;
        long lastAttemptLatency = 0;

        while (attempts <= maxRetries) {
            long attemptStart = System.currentTimeMillis();
            attempts++;

            try {
                HttpEntity<String> httpEntity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity(chatUrl, httpEntity, Map.class);
                lastAttemptLatency = System.currentTimeMillis() - attemptStart;
                long totalLatency = System.currentTimeMillis() - overallStartTime;

                // Track rate limit headers
                if (rateLimitTracker != null) {
                    rateLimitTracker.updateFromHeaders(responseEntity.getHeaders());
                }

                Map body = responseEntity.getBody();
                String extractedText = extractText(body);
                String returnedModel = (body != null && body.containsKey("model"))
                        ? String.valueOf(body.get("model"))
                        : effectiveModel;

                // Extract token usage
                int promptTokens = 0;
                int completionTokens = 0;
                int totalTokens = 0;
                if (body != null && body.get("usage") instanceof Map) {
                    Map<?, ?> usage = (Map<?, ?>) body.get("usage");
                    promptTokens = getInt(usage, "prompt_tokens");
                    completionTokens = getInt(usage, "completion_tokens");
                    totalTokens = getInt(usage, "total_tokens");
                }

                if (extractedText != null && !extractedText.trim().isEmpty()) {
                    if (usageMetricsService != null) {
                        usageMetricsService.recordRequest(request.getUserIdentifier(), true, false, promptTokens, completionTokens, totalLatency);
                    }
                    return AiProviderResponse.success(extractedText.trim(), returnedModel,
                            promptTokens, completionTokens, totalTokens, totalLatency, flattenHeaders(responseEntity.getHeaders()));
                }

                return AiProviderResponse.failure("Empty response received from Groq (" + effectiveModel + ")",
                        effectiveModel, 502, totalLatency, flattenHeaders(responseEntity.getHeaders()));

            } catch (HttpStatusCodeException e) {
                lastAttemptLatency = System.currentTimeMillis() - attemptStart;
                long totalLatency = System.currentTimeMillis() - overallStartTime;

                // Update rate limit tracker from response headers even on errors
                if (rateLimitTracker != null) {
                    rateLimitTracker.updateFromHeaders(e.getResponseHeaders());
                }

                int statusCode = e.getStatusCode().value();
                String errorMsg = extractErrorMessage(e);

                // Handle HTTP 429 Rate Limit with exponential backoff and jitter
                if (statusCode == 429 && attempts <= maxRetries) {
                    long backoffDelay = computeBackoffDelay(attempts, e.getResponseHeaders());
                    try {
                        Thread.sleep(backoffDelay);
                        continue; // Retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return AiProviderResponse.failure("Request interrupted during rate limit backoff",
                                effectiveModel, 429, totalLatency, flattenHeaders(e.getResponseHeaders()));
                    }
                }

                // If non-retryable or max retries exceeded
                if (usageMetricsService != null) {
                    usageMetricsService.recordRequest(request.getUserIdentifier(), false, statusCode == 429, 0, 0, totalLatency);
                }

                return AiProviderResponse.failure(errorMsg, effectiveModel, statusCode, totalLatency, flattenHeaders(e.getResponseHeaders()));

            } catch (Exception e) {
                lastAttemptLatency = System.currentTimeMillis() - attemptStart;
                long totalLatency = System.currentTimeMillis() - overallStartTime;
                if (usageMetricsService != null) {
                    usageMetricsService.recordRequest(request.getUserIdentifier(), false, false, 0, 0, totalLatency);
                }
                return AiProviderResponse.failure("Groq connection failure: " + e.getMessage(),
                        effectiveModel, 500, totalLatency, Collections.emptyMap());
            }
        }

        return AiProviderResponse.failure("Maximum retries (" + maxRetries + ") exceeded for Groq request",
                effectiveModel, 429, System.currentTimeMillis() - overallStartTime, Collections.emptyMap());
    }

    private long computeBackoffDelay(int attempt, HttpHeaders headers) {
        // 1. Check Retry-After header
        if (headers != null) {
            String retryAfter = headers.getFirst("retry-after");
            if (retryAfter != null && !retryAfter.trim().isEmpty()) {
                try {
                    long sec = Long.parseLong(retryAfter.trim());
                    if (sec > 0) return (sec * 1000) + ThreadLocalRandom.current().nextInt(100, 300);
                } catch (Exception ignored) {}
            }
        }

        // 2. Exponential backoff with jitter
        long exp = baseBackoffMs * (1L << (attempt - 1));
        long delay = Math.min(maxBackoffMs, exp);
        long jitter = ThreadLocalRandom.current().nextLong(100, 400);
        return delay + jitter;
    }

    private String resolveChatUrl() {
        String base = (groqApiUrl != null && !groqApiUrl.trim().isEmpty()) ? groqApiUrl.trim() : "https://api.groq.com/openai/v1";
        if (base.endsWith("/chat/completions")) return base;
        if (base.endsWith("/")) return base + "chat/completions";
        return base + "/chat/completions";
    }

    private String extractText(Map response) {
        if (response == null) return null;
        if (response.containsKey("choices") && response.get("choices") instanceof List) {
            List<?> choices = (List<?>) response.get("choices");
            if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                if (firstChoice.containsKey("message") && firstChoice.get("message") instanceof Map) {
                    Map<?, ?> msg = (Map<?, ?>) firstChoice.get("message");
                    if (msg.containsKey("content") && msg.get("content") != null) {
                        return String.valueOf(msg.get("content")).trim();
                    }
                }
            }
        }
        return null;
    }

    private String extractErrorMessage(HttpStatusCodeException e) {
        String defaultMsg = "HTTP " + e.getStatusCode().value() + " " + e.getStatusText();
        try {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                Map errJson = objectMapper.readValue(responseBody, Map.class);
                if (errJson != null && errJson.containsKey("error")) {
                    Object errObj = errJson.get("error");
                    if (errObj instanceof Map) {
                        Map errMap = (Map) errObj;
                        if (errMap.containsKey("message")) {
                            return String.valueOf(errMap.get("message"));
                        }
                    } else if (errObj instanceof String) {
                        return (String) errObj;
                    }
                }
            }
        } catch (Exception ignored) {}
        return defaultMsg;
    }

    private Map<String, String> flattenHeaders(HttpHeaders headers) {
        if (headers == null) return Collections.emptyMap();
        Map<String, String> map = new HashMap<>();
        headers.forEach((k, v) -> {
            if (k != null && v != null && !v.isEmpty()) {
                map.put(k.toLowerCase(), v.get(0));
            }
        });
        return map;
    }

    private int getInt(Map<?, ?> map, String key) {
        if (map == null || !map.containsKey(key) || map.get(key) == null) return 0;
        try {
            return Integer.parseInt(map.get(key).toString());
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Override
    public AiProviderResponse testConnection(String model) {
        if (!isConfigured()) {
            return AiProviderResponse.failure("Groq API key is not configured", groqModel, 503, 0, Collections.emptyMap());
        }
        String targetModel = (model != null && !model.trim().isEmpty()) ? model.trim() : groqModel;
        AiProviderRequest probe = new AiProviderRequest(
                null,
                null,
                Collections.singletonList(Map.of("role", "user", "content", "Reply with exactly: Groq connection successful.")),
                targetModel,
                50,
                0.0,
                "health-probe"
        );
        return executeChat(probe);
    }
}
