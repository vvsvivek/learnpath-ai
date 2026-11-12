package com.vrsec.backend.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Tracks AI request usage metrics, latency, token consumption,
 * and compiles comprehensive administrative usage dashboards.
 */
@Service
public class AiUsageMetricsService {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong rateLimitedRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);

    private final AtomicLong totalPromptTokens = new AtomicLong(0);
    private final AtomicLong totalCompletionTokens = new AtomicLong(0);
    private final AtomicLong totalTokens = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    // Sliding window of token consumption (timestamp -> tokens)
    private final Queue<TokenTimestamp> recentTokens = new ConcurrentLinkedQueue<>();

    // Per-user usage breakdown
    private final ConcurrentHashMap<String, UserMetrics> userMetricsMap = new ConcurrentHashMap<>();

    @Autowired private AiRateLimitTracker rateLimitTracker;
    @Autowired private AiConcurrencyLimiter concurrencyLimiter;

    public static class TokenTimestamp {
        final long timestamp;
        final int tokens;

        public TokenTimestamp(long timestamp, int tokens) {
            this.timestamp = timestamp;
            this.tokens = tokens;
        }
    }

    public static class UserMetrics {
        private final AtomicLong requests = new AtomicLong(0);
        private final AtomicLong tokens = new AtomicLong(0);
        private volatile LocalDateTime lastActive = LocalDateTime.now();

        public void record(int tokensUsed) {
            requests.incrementAndGet();
            tokens.addAndGet(tokensUsed);
            lastActive = LocalDateTime.now();
        }

        public long getRequests() { return requests.get(); }
        public long getTokens() { return tokens.get(); }
        public LocalDateTime getLastActive() { return lastActive; }
    }

    /**
     * Records a completed request.
     */
    public void recordRequest(
            String userIdentifier,
            boolean success,
            boolean rateLimited,
            int promptTokens,
            int completionTokens,
            long latencyMs) {
        totalRequests.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);

        if (success) {
            successfulRequests.incrementAndGet();
        } else if (rateLimited) {
            rateLimitedRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }

        int reqTokens = promptTokens + completionTokens;
        if (reqTokens > 0) {
            totalPromptTokens.addAndGet(promptTokens);
            totalCompletionTokens.addAndGet(completionTokens);
            totalTokens.addAndGet(reqTokens);

            long now = System.currentTimeMillis();
            recentTokens.add(new TokenTimestamp(now, reqTokens));

            // Purge tokens older than 60 seconds
            long windowStart = now - 60000;
            while (!recentTokens.isEmpty()) {
                TokenTimestamp peek = recentTokens.peek();
                if (peek != null && peek.timestamp < windowStart) {
                    recentTokens.poll();
                } else {
                    break;
                }
            }
        }

        if (userIdentifier != null && !userIdentifier.trim().isEmpty()) {
            String clean = userIdentifier.trim().toLowerCase();
            userMetricsMap.computeIfAbsent(clean, k -> new UserMetrics()).record(reqTokens);
        }
    }

    /**
     * Calculates rolling 60-second token consumption.
     */
    public long getRollingTokensPerMinute() {
        long now = System.currentTimeMillis();
        long windowStart = now - 60000;
        long sum = 0;
        for (TokenTimestamp tt : recentTokens) {
            if (tt.timestamp >= windowStart) {
                sum += tt.tokens;
            }
        }
        return sum;
    }

    /**
     * Generates an administrative usage and capacity dashboard.
     */
    public Map<String, Object> getAdminDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        long totReq = totalRequests.get();
        long succReq = successfulRequests.get();
        double successRate = totReq > 0 ? ((double) succReq / totReq) * 100.0 : 100.0;
        double avgLatency = totReq > 0 ? (double) totalLatencyMs.get() / totReq : 0.0;

        // Concurrency & Queue Status
        Map<String, Object> capacity = new LinkedHashMap<>();
        capacity.put("activeInFlight", concurrencyLimiter != null ? concurrencyLimiter.getInFlightCount() : 0);
        capacity.put("concurrencyLimit", concurrencyLimiter != null ? concurrencyLimiter.getConcurrencyLimit() : 2);
        capacity.put("queuedRequests", concurrencyLimiter != null ? concurrencyLimiter.getQueuedCount() : 0);
        capacity.put("queueCapacity", concurrencyLimiter != null ? concurrencyLimiter.getQueueCapacity() : 20);
        dashboard.put("capacity", capacity);

        // Provider Rate Limit Status (Headers)
        Map<String, Object> rateLimits = rateLimitTracker != null ? rateLimitTracker.getRateLimitSnapshot() : Collections.emptyMap();
        dashboard.put("providerRateLimits", rateLimits);

        // Request Statistics
        Map<String, Object> requests = new LinkedHashMap<>();
        requests.put("totalRequests", totReq);
        requests.put("successfulRequests", succReq);
        requests.put("rateLimitedRequests", rateLimitedRequests.get());
        requests.put("failedRequests", failedRequests.get());
        requests.put("successRatePercent", String.format("%.1f%%", successRate));
        requests.put("averageLatencyMs", String.format("%.0f ms", avgLatency));
        dashboard.put("requests", requests);

        // Token Consumption Statistics
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("totalTokens", totalTokens.get());
        tokens.put("promptTokens", totalPromptTokens.get());
        tokens.put("completionTokens", totalCompletionTokens.get());
        tokens.put("rollingTokensLastMinute", getRollingTokensPerMinute());
        dashboard.put("tokens", tokens);

        // Top Active Users
        List<Map<String, Object>> topUsers = userMetricsMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().getTokens(), a.getValue().getTokens()))
                .limit(10)
                .map(entry -> {
                    Map<String, Object> u = new LinkedHashMap<>();
                    u.put("user", entry.getKey());
                    u.put("requests", entry.getValue().getRequests());
                    u.put("tokens", entry.getValue().getTokens());
                    u.put("lastActive", entry.getValue().getLastActive().toString());
                    return u;
                })
                .collect(Collectors.toList());
        dashboard.put("topUsers", topUsers);

        return dashboard;
    }
}
