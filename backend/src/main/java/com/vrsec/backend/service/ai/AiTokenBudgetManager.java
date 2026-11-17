package com.vrsec.backend.service.ai;

import com.vrsec.backend.model.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Manages token budgets, sliding window conversation history pruning,
 * and stable system prompt formatting to maximize Groq prompt cache hits.
 */
@Service
public class AiTokenBudgetManager {

    private final int maxHistoryTurns;
    private final int maxPromptTokens;
    private final int maxCompletionTokens;
    private final int maxHistoryMessageLength;

    public AiTokenBudgetManager(
            @Value("${groq.max-history-turns:2}") int maxHistoryTurns,
            @Value("${groq.max-prompt-tokens:2500}") int maxPromptTokens,
            @Value("${groq.max-completion-tokens:1024}") int maxCompletionTokens,
            @Value("${groq.max-history-message-length:600}") int maxHistoryMessageLength) {
        this.maxHistoryTurns = Math.max(1, maxHistoryTurns);
        this.maxPromptTokens = Math.max(500, maxPromptTokens);
        this.maxCompletionTokens = Math.max(128, maxCompletionTokens);
        this.maxHistoryMessageLength = Math.max(100, maxHistoryMessageLength);
    }

    /**
     * Prunes and builds a lean conversational contents list.
     * Restricts history to the most recent turns and caps message lengths
     * to protect against token exhaustion on the 8000 TPM tier.
     */
    public List<Map<String, Object>> buildOptimizedConversation(
            List<ChatMessage> pastMessages,
            String newUserMessage,
            String dynamicContext) {
        List<Map<String, Object>> contents = new ArrayList<>();

        int maxHistoryMessages = maxHistoryTurns * 2; // e.g. 2 turns = 4 messages (user, assistant, user, assistant)

        if (pastMessages != null && !pastMessages.isEmpty()) {
            int startIdx = Math.max(0, pastMessages.size() - maxHistoryMessages);
            List<ChatMessage> recent = pastMessages.subList(startIdx, pastMessages.size());

            String lastRole = null;
            for (int i = 0; i < recent.size(); i++) {
                ChatMessage m = recent.get(i);
                String sender = m.getSender();
                String role = (sender != null && sender.equalsIgnoreCase("user")) ? "user" : "assistant";
                String text = m.getContent();
                if (text == null || text.trim().isEmpty()) continue;

                // If this is the last message in history, and it is a user message without an assistant reply
                // (e.g. from an earlier failed attempt), skip it if user is resending or to prevent consecutive user turns
                if (i == recent.size() - 1 && "user".equals(role)) {
                    continue;
                }

                // Skip consecutive duplicate roles
                if (role.equals(lastRole)) {
                    continue;
                }

                // If history starts with assistant, skip so initial turn is user
                if (contents.isEmpty() && "assistant".equals(role)) {
                    continue;
                }

                // Truncate older lengthy turns to protect TPM budget
                String cappedText = text.trim();
                if (cappedText.length() > maxHistoryMessageLength) {
                    cappedText = cappedText.substring(0, maxHistoryMessageLength) + "... [context truncated for length]";
                }

                contents.add(Map.of(
                        "role", role,
                        "content", cappedText
                ));
                lastRole = role;
            }
        }

        // Construct final user prompt, injecting dynamic context as a labeled section
        // to maintain strict prefix caching on the system message
        String finalContent = newUserMessage.trim();
        if (dynamicContext != null && !dynamicContext.trim().isEmpty()) {
            finalContent = "[ACADEMIC CONTEXT]\n" + dynamicContext.trim() + "\n[/ACADEMIC CONTEXT]\n\n" + finalContent;
        }

        contents.add(Map.of(
                "role", "user",
                "content", finalContent
        ));

        return contents;
    }

    /**
     * Estimates token count for a text string (~4 chars per token).
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 3.8);
    }

    public int getMaxPromptTokens() {
        return maxPromptTokens;
    }

    public int getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public int getMaxHistoryTurns() {
        return maxHistoryTurns;
    }
}
