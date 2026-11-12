package com.vrsec.backend.service.ai;

import java.util.Collections;
import java.util.Map;

/**
 * Encapsulates the execution result of an AI prompt from any provider.
 */
public class AiProviderResponse {

    private final boolean success;
    private final String text;
    private final String modelVersion;
    private final String errorMessage;
    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;
    private final long latencyMs;
    private final int httpStatusCode;
    private final Map<String, String> rateLimitHeaders;

    public AiProviderResponse(
            boolean success,
            String text,
            String modelVersion,
            String errorMessage,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long latencyMs,
            int httpStatusCode,
            Map<String, String> rateLimitHeaders) {
        this.success = success;
        this.text = text;
        this.modelVersion = modelVersion;
        this.errorMessage = errorMessage;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.latencyMs = latencyMs;
        this.httpStatusCode = httpStatusCode;
        this.rateLimitHeaders = rateLimitHeaders != null ? rateLimitHeaders : Collections.emptyMap();
    }

    public static AiProviderResponse success(
            String text,
            String modelVersion,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long latencyMs,
            Map<String, String> rateLimitHeaders) {
        return new AiProviderResponse(true, text, modelVersion, null,
                promptTokens, completionTokens, totalTokens, latencyMs, 200, rateLimitHeaders);
    }

    public static AiProviderResponse failure(
            String errorMessage,
            String modelVersion,
            int httpStatusCode,
            long latencyMs,
            Map<String, String> rateLimitHeaders) {
        return new AiProviderResponse(false, null, modelVersion, errorMessage,
                0, 0, 0, latencyMs, httpStatusCode, rateLimitHeaders);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getText() {
        return text;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public Map<String, String> getRateLimitHeaders() {
        return rateLimitHeaders;
    }
}
