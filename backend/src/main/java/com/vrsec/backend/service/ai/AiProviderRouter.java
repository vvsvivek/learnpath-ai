package com.vrsec.backend.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Routes AI requests to the active configured LLM provider
 * (Groq Priority 1, OpenAI Fallback, Gemini Fallback).
 */
@Service
public class AiProviderRouter {

    @Autowired private GroqAiProvider groqProvider;
    @Autowired private OpenAiProvider openAiProvider;
    @Autowired private GeminiAiProvider geminiProvider;

    /**
     * Resolves the primary active provider.
     */
    public AiProvider getActiveProvider() {
        if (groqProvider != null && groqProvider.isConfigured()) {
            return groqProvider;
        }
        if (openAiProvider != null && openAiProvider.isConfigured()) {
            return openAiProvider;
        }
        if (geminiProvider != null && geminiProvider.isConfigured()) {
            return geminiProvider;
        }
        // Return groqProvider by default so its error messages (e.g. key missing) are returned
        return groqProvider;
    }

    /**
     * Executes chat completion on the active provider.
     */
    public AiProviderResponse executeChat(AiProviderRequest request) {
        AiProvider provider = getActiveProvider();
        if (provider == null || !provider.isConfigured()) {
            return AiProviderResponse.failure("No AI provider is configured in backend environment",
                    "none", 503, 0, Collections.emptyMap());
        }
        return provider.executeChat(request);
    }

    /**
     * Executes test probe on the active provider.
     */
    public AiProviderResponse testConnection(String model) {
        AiProvider provider = getActiveProvider();
        if (provider == null || !provider.isConfigured()) {
            return AiProviderResponse.failure("No AI provider is configured", "none", 503, 0, Collections.emptyMap());
        }
        return provider.testConnection(model);
    }

    public Map<String, Object> getProvidersStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        AiProvider active = getActiveProvider();
        status.put("activeProvider", active != null ? active.getProviderName() : "NONE");
        status.put("groqConfigured", groqProvider != null && groqProvider.isConfigured());
        status.put("openaiConfigured", openAiProvider != null && openAiProvider.isConfigured());
        status.put("geminiConfigured", geminiProvider != null && geminiProvider.isConfigured());
        return status;
    }
}
