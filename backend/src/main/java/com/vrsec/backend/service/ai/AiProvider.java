package com.vrsec.backend.service.ai;

/**
 * Universal abstraction for external AI LLM providers (Groq, OpenAI, Gemini).
 */
public interface AiProvider {

    /**
     * Unique identifier for the provider (e.g. "GROQ", "OPENAI", "GEMINI").
     */
    String getProviderName();

    /**
     * Checks whether the provider's API key is configured.
     */
    boolean isConfigured();

    /**
     * Executes a chat/prompt completion request.
     */
    AiProviderResponse executeChat(AiProviderRequest request);

    /**
     * Executes a minimal verification probe to verify credentials and model availability.
     */
    AiProviderResponse testConnection(String model);
}
