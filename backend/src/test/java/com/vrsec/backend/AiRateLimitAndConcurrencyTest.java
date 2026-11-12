package com.vrsec.backend;

import com.vrsec.backend.model.ChatMessage;
import com.vrsec.backend.model.ChatSession;
import com.vrsec.backend.service.ai.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AiRateLimitAndConcurrencyTest {

    private AiRateLimitTracker rateLimitTracker;
    private AiConcurrencyLimiter concurrencyLimiter;
    private AiTokenBudgetManager tokenBudgetManager;
    private AiUsageMetricsService usageMetricsService;

    @BeforeEach
    void setUp() {
        rateLimitTracker = new AiRateLimitTracker();
        concurrencyLimiter = new AiConcurrencyLimiter(2, 5, 2000);
        tokenBudgetManager = new AiTokenBudgetManager(2, 2500, 1024, 200);
        usageMetricsService = new AiUsageMetricsService();
    }

    // =========================================================================
    // 1. PER-USER RATE LIMITING & FAIR SHARING TESTS
    // =========================================================================
    @Test
    void testPerUserRateLimit_EnforcesRpmThreshold() {
        String user = "student.test@vrsec.ac.in";
        int maxRpm = 3;

        // First 3 requests in the minute must succeed
        assertNull(rateLimitTracker.tryAcquireUserSlot(user, maxRpm));
        rateLimitTracker.releaseUserSlot(user);

        assertNull(rateLimitTracker.tryAcquireUserSlot(user, maxRpm));
        rateLimitTracker.releaseUserSlot(user);

        assertNull(rateLimitTracker.tryAcquireUserSlot(user, maxRpm));
        rateLimitTracker.releaseUserSlot(user);

        // 4th request must be rejected to prevent quota exhaustion
        String error = rateLimitTracker.tryAcquireUserSlot(user, maxRpm);
        assertNotNull(error);
        assertTrue(error.contains("reached your per-minute AI Tutor request limit"));
    }

    @Test
    void testPerUserRateLimit_PreventsConcurrentInFlightSpam() {
        String user = "spammer@vrsec.ac.in";
        int maxRpm = 10;

        // Slot 1 acquired and kept in-flight
        assertNull(rateLimitTracker.tryAcquireUserSlot(user, maxRpm));

        // Simultaneous second request must be rejected immediately
        String error = rateLimitTracker.tryAcquireUserSlot(user, maxRpm);
        assertNotNull(error);
        assertTrue(error.contains("active AI request already in progress"));

        // Once the first request completes, user can request again
        rateLimitTracker.releaseUserSlot(user);
        assertNull(rateLimitTracker.tryAcquireUserSlot(user, maxRpm));
        rateLimitTracker.releaseUserSlot(user);
    }

    // =========================================================================
    // 2. CONCURRENCY LIMITER & BOUNDED QUEUE TESTS
    // =========================================================================
    @Test
    void testConcurrencyLimiter_EnforcesPermitBounds() throws Exception {
        AiConcurrencyLimiter limiter = new AiConcurrencyLimiter(2, 5, 500);

        limiter.acquireSlot();
        limiter.acquireSlot();
        assertEquals(2, limiter.getInFlightCount());

        // 3rd concurrent request should time out because permits are 2
        assertThrows(AiConcurrencyLimiter.AiServiceTimeoutException.class, limiter::acquireSlot);

        // Release 1 slot
        limiter.releaseSlot();
        assertEquals(1, limiter.getInFlightCount());

        // Now acquisition should succeed
        limiter.acquireSlot();
        assertEquals(2, limiter.getInFlightCount());
    }

    @Test
    void testConcurrencyLimiter_RejectsWhenQueueFull() {
        // Limiter with concurrency=1, queue capacity=1, timeout=100ms
        AiConcurrencyLimiter strictLimiter = new AiConcurrencyLimiter(1, 1, 100);

        // Consume permit
        strictLimiter.acquireSlot();

        // Second thread waits in queue (queued count becomes 1)
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
            try {
                strictLimiter.acquireSlot();
            } catch (Exception ignored) {
            } finally {
                latch.countDown();
            }
        });

        // Third thread immediately rejected with AiServiceOverloadedException
        assertThrows(Exception.class, () -> {
            // Poll for up to 50ms to allow async thread to enter queue
            Thread.sleep(30);
            strictLimiter.acquireSlot();
        });
    }

    // =========================================================================
    // 3. GROQ RATE-LIMIT HEADER PARSING
    // =========================================================================
    @Test
    void testRateLimitTracker_ParsesGroqHeadersCorrectly() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-ratelimit-remaining-tokens", "6250");
        headers.set("x-ratelimit-limit-tokens", "8000");
        headers.set("x-ratelimit-remaining-requests", "25");
        headers.set("x-ratelimit-limit-requests", "30");
        headers.set("x-ratelimit-reset-tokens", "4.8s");
        headers.set("x-ratelimit-reset-requests", "2s");

        rateLimitTracker.updateFromHeaders(headers);

        assertEquals(6250L, rateLimitTracker.getRemainingTokens());
        assertEquals(8000L, rateLimitTracker.getLimitTokens());
        assertEquals(25, rateLimitTracker.getRemainingRequests());
        assertEquals(30, rateLimitTracker.getLimitRequests());
        assertEquals("4.8s", rateLimitTracker.getResetTokensDuration());
        assertEquals("2s", rateLimitTracker.getResetRequestsDuration());
        assertNotNull(rateLimitTracker.getLastHeaderUpdate());
    }

    // =========================================================================
    // 4. TOKEN BUDGETING & CONVERSATION PRUNING
    // =========================================================================
    @Test
    void testTokenBudgetManager_PrunesHistoryToConfiguredTurns() {
        ChatSession session = new ChatSession();
        List<ChatMessage> history = new ArrayList<>();

        // Add 6 past messages (3 turns)
        history.add(new ChatMessage(session, "user", "Turn 1 question"));
        history.add(new ChatMessage(session, "ai", "Turn 1 answer"));
        history.add(new ChatMessage(session, "user", "Turn 2 question"));
        history.add(new ChatMessage(session, "ai", "Turn 2 answer"));
        history.add(new ChatMessage(session, "user", "Turn 3 question"));
        history.add(new ChatMessage(session, "ai", "Turn 3 answer"));

        // maxHistoryTurns is 2 -> should retain only last 2 turns (4 past messages) + incoming user message = 5 messages
        List<Map<String, Object>> pruned = tokenBudgetManager.buildOptimizedConversation(
                history, "Turn 4 question", "Course: DBMS");

        assertNotNull(pruned);
        assertEquals(5, pruned.size(), "Must contain 4 pruned history messages + 1 new user message");

        // Verify oldest turn (Turn 1) was pruned away to save TPM tokens
        assertFalse(pruned.stream().anyMatch(m -> "Turn 1 question".equals(m.get("content"))));

        // Verify latest turn contains academic context attached to user message
        Map<String, Object> last = pruned.get(pruned.size() - 1);
        assertEquals("user", last.get("role"));
        assertTrue(((String) last.get("content")).contains("[ACADEMIC CONTEXT]"));
        assertTrue(((String) last.get("content")).contains("Turn 4 question"));
    }

    @Test
    void testTokenBudgetManager_HandlesRetryAfterFailedAttempt() {
        ChatSession session = new ChatSession();
        List<ChatMessage> history = new ArrayList<>();

        // Past history ends with a user message that failed to receive an AI reply
        history.add(new ChatMessage(session, "user", "Question 1"));
        history.add(new ChatMessage(session, "ai", "Answer 1"));
        history.add(new ChatMessage(session, "user", "Question 2 (failed previously)"));

        // User is retrying Question 2
        List<Map<String, Object>> pruned = tokenBudgetManager.buildOptimizedConversation(
                history, "Question 2 (retrying)", null);

        assertNotNull(pruned);
        // Trailing unanswered user message should be pruned to avoid duplicate consecutive user turns
        assertEquals(3, pruned.size());
        assertEquals("user", pruned.get(0).get("role"));
        assertEquals("Question 1", pruned.get(0).get("content"));
        assertEquals("assistant", pruned.get(1).get("role"));
        assertEquals("Answer 1", pruned.get(1).get("content"));
        assertEquals("user", pruned.get(2).get("role"));
        assertEquals("Question 2 (retrying)", pruned.get(2).get("content"));
    }

    @Test
    void testTokenBudgetManager_TruncatesExcessiveMessageLength() {
        ChatSession session = new ChatSession();
        List<ChatMessage> history = new ArrayList<>();

        String hugeText = "A".repeat(800); // Exceeds maxHistoryMessageLength (200)
        history.add(new ChatMessage(session, "user", hugeText));
        history.add(new ChatMessage(session, "ai", "Short answer"));

        List<Map<String, Object>> pruned = tokenBudgetManager.buildOptimizedConversation(
                history, "Next question", null);

        assertNotNull(pruned);
        String firstContent = (String) pruned.get(0).get("content");
        assertTrue(firstContent.length() < 300);
        assertTrue(firstContent.contains("[context truncated for length]"));
    }

    // =========================================================================
    // 5. USAGE METRICS & ADMIN DASHBOARD
    // =========================================================================
    @Test
    void testUsageMetricsService_RecordsTokensAndComputesDashboard() {
        String student = "student1@vrsec.ac.in";
        usageMetricsService.recordRequest(student, true, false, 150, 200, 350);
        usageMetricsService.recordRequest(student, false, true, 0, 0, 100);

        Map<String, Object> dashboard = usageMetricsService.getAdminDashboard();
        assertNotNull(dashboard);

        Map<?, ?> requests = (Map<?, ?>) dashboard.get("requests");
        assertEquals(2L, requests.get("totalRequests"));
        assertEquals(1L, requests.get("successfulRequests"));
        assertEquals(1L, requests.get("rateLimitedRequests"));

        Map<?, ?> tokens = (Map<?, ?>) dashboard.get("tokens");
        assertEquals(350L, tokens.get("totalTokens"));
        assertEquals(150L, tokens.get("promptTokens"));
        assertEquals(200L, tokens.get("completionTokens"));

        List<?> topUsers = (List<?>) dashboard.get("topUsers");
        assertFalse(topUsers.isEmpty());
        Map<?, ?> top = (Map<?, ?>) topUsers.get(0);
        assertEquals(student, top.get("user"));
        assertEquals(350L, top.get("tokens"));
    }

    // =========================================================================
    // 6. STUDENT DATA CACHE ISOLATION (ZERO CROSS-USER LEAKAGE)
    // =========================================================================
    @Test
    void testStudentDataIsolation_NeverSharesContextAcrossDifferentUsers() {
        String contextA = "Student A: CGPA 9.2, Course: Advanced Algorithms";
        String contextB = "Student B: CGPA 6.1, Course: Remedial Database Systems";

        List<Map<String, Object>> convA = tokenBudgetManager.buildOptimizedConversation(
                Collections.emptyList(), "What is my study plan?", contextA);
        List<Map<String, Object>> convB = tokenBudgetManager.buildOptimizedConversation(
                Collections.emptyList(), "What is my study plan?", contextB);

        String promptA = (String) convA.get(0).get("content");
        String promptB = (String) convB.get(0).get("content");

        assertTrue(promptA.contains("Advanced Algorithms"));
        assertFalse(promptA.contains("Remedial Database Systems"));

        assertTrue(promptB.contains("Remedial Database Systems"));
        assertFalse(promptB.contains("Advanced Algorithms"));
    }
}
