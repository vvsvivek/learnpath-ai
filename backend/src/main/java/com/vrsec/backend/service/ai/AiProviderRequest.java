package com.vrsec.backend.service.ai;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates a provider-neutral AI prompt request.
 */
public class AiProviderRequest {

    private final String systemPromptPrefix;
    private final String dynamicContext;
    private final List<Map<String, Object>> messages;
    private final String targetModel;
    private final int maxCompletionTokens;
    private final double temperature;
    private final String userIdentifier;

    public AiProviderRequest(
            String systemPromptPrefix,
            String dynamicContext,
            List<Map<String, Object>> messages,
            String targetModel,
            int maxCompletionTokens,
            double temperature,
            String userIdentifier) {
        this.systemPromptPrefix = systemPromptPrefix != null ? systemPromptPrefix.trim() : "";
        this.dynamicContext = dynamicContext != null ? dynamicContext.trim() : "";
        this.messages = messages != null ? messages : Collections.emptyList();
        this.targetModel = targetModel != null ? targetModel.trim() : "";
        this.maxCompletionTokens = maxCompletionTokens > 0 ? maxCompletionTokens : 1024;
        this.temperature = temperature >= 0 ? temperature : 0.7;
        this.userIdentifier = userIdentifier != null ? userIdentifier.trim().toLowerCase() : "anonymous";
    }

    public String getSystemPromptPrefix() {
        return systemPromptPrefix;
    }

    public String getDynamicContext() {
        return dynamicContext;
    }

    public List<Map<String, Object>> getMessages() {
        return messages;
    }

    public String getTargetModel() {
        return targetModel;
    }

    public int getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }
}
