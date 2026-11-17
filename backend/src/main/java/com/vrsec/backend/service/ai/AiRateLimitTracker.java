package com.vrsec.backend.service.ai;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks provider rate limits (Groq TPM/RPM) and enforces per-user quotas
 * to prevent individual users from exhausting the shared 8000 TPM limit.
 */
@Service
public class AiRateLimitTracker {

    // Provider-level rate limits (updated dynamically from Groq response headers)
    private final AtomicLong limitTokens = new AtomicLong(8000);
    private final AtomicLong remainingTokens = new AtomicLong(8000);
    private final AtomicInteger limitRequests = new AtomicInteger(30);
    private final AtomicInteger remainingRequests = new AtomicInteger(30);
    private volatile String resetTokensDuration = "0s";
    private volatile String resetRequestsDuration = "0s";
    private volatile LocalDateTime lastHeaderUpdate = null;

    // Per-user sliding window request tracking (Fair sharing protection)
    private final ConcurrentHashMap<String, UserRateLimitState> userStates = new ConcurrentHashMap<>();

    public static class UserRateLimitState {
        private final AtomicInteger inFlight = new AtomicInteger(0);
        private final Queue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();

        public AtomicInteger getInFlight() {
            return inFlight;
        }

        public Queue<Long> getRequestTimestamps() {
            return requestTimestamps;
        }
    }

    /**
     * Inspects and parses rate limit headers returned by Groq.
     */
    public void updateFromHeaders(HttpHeaders headers) {
        if (headers == null) return;

        try {
            String remTok = headers.getFirst("x-ratelimit-remaining-tokens");
            if (remTok != null && !remTok.trim().isEmpty()) {
                remainingTokens.set(Long.parseLong(remTok.trim()));
            }

            String limTok = headers.getFirst("x-ratelimit-limit-tokens");
            if (limTok != null && !limTok.trim().isEmpty()) {
                limitTokens.set(Long.parseLong(limTok.trim()));
            }

            String remReq = headers.getFirst("x-ratelimit-remaining-requests");
            if (remReq != null && !remReq.trim().isEmpty()) {
                remainingRequests.set(Integer.parseInt(remReq.trim()));
            }

            String limReq = headers.getFirst("x-ratelimit-limit-requests");
            if (limReq != null && !limReq.trim().isEmpty()) {
                limitRequests.set(Integer.parseInt(limReq.trim()));
            }

            String rstTok = headers.getFirst("x-ratelimit-reset-tokens");
            if (rstTok != null && !rstTok.trim().isEmpty()) {
                resetTokensDuration = rstTok.trim();
            }

            String rstReq = headers.getFirst("x-ratelimit-reset-requests");
            if (rstReq != null && !rstReq.trim().isEmpty()) {
                resetRequestsDuration = rstReq.trim();
            }

            lastHeaderUpdate = LocalDateTime.now();
        } catch (Exception ignored) {
            // Header parsing failures should never crash application flow
        }
    }

    /**
     * Attempts to acquire a rate-limited slot for an individual user.
     * Prevents parallel spam from the same user (max 1 concurrent in-flight per user)
     * and bounds per-minute request rate (maxRpm per user).
     *
     * @return null if allowed, or an error message if user quota is exceeded.
     */
    public String tryAcquireUserSlot(String userIdentifier, int maxRpm) {
        if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
            userIdentifier = "anonymous";
        }
        String cleanUser = userIdentifier.trim().toLowerCase();
        UserRateLimitState state = userStates.computeIfAbsent(cleanUser, k -> new UserRateLimitState());

        // 1. Prevent concurrent overlapping requests from the same user
        if (state.inFlight.get() > 0) {
            return "You have an active AI request already in progress. Please wait for it to complete.";
        }

        // 2. Sliding window check: purge timestamps older than 60 seconds
        long now = System.currentTimeMillis();
        long windowStart = now - 60000;
        while (!state.requestTimestamps.isEmpty()) {
            Long earliest = state.requestTimestamps.peek();
            if (earliest != null && earliest < windowStart) {
                state.requestTimestamps.poll();
            } else {
                break;
            }
        }

        // 3. Enforce per-user RPM limit
        int currentRpm = state.requestTimestamps.size();
        if (currentRpm >= maxRpm) {
            Long earliest = state.requestTimestamps.peek();
            long waitMs = earliest != null ? Math.max(1000, 60000 - (now - earliest)) : 30000;
            long waitSec = (waitMs + 999) / 1000;
            return "You have reached your per-minute AI Tutor request limit (" + maxRpm + " queries/min). Please wait " + waitSec + " seconds before retrying.";
        }

        // 4. Reserve slot
        state.requestTimestamps.add(now);
        state.inFlight.incrementAndGet();
        return null;
    }

    /**
     * Releases the in-flight lock for the given user upon completion of the request.
     */
    public void releaseUserSlot(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
            userIdentifier = "anonymous";
        }
        String cleanUser = userIdentifier.trim().toLowerCase();
        UserRateLimitState state = userStates.get(cleanUser);
        if (state != null) {
            state.inFlight.updateAndGet(current -> Math.max(0, current - 1));
        }
    }

    public long getRemainingTokens() {
        return remainingTokens.get();
    }

    public long getLimitTokens() {
        return limitTokens.get();
    }

    public int getRemainingRequests() {
        return remainingRequests.get();
    }

    public int getLimitRequests() {
        return limitRequests.get();
    }

    public String getResetTokensDuration() {
        return resetTokensDuration;
    }

    public String getResetRequestsDuration() {
        return resetRequestsDuration;
    }

    public LocalDateTime getLastHeaderUpdate() {
        return lastHeaderUpdate;
    }

    public int getActiveUserCount() {
        return userStates.size();
    }

    public Map<String, Object> getRateLimitSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("remainingTokens", remainingTokens.get());
        snapshot.put("limitTokens", limitTokens.get());
        snapshot.put("remainingRequests", remainingRequests.get());
        snapshot.put("limitRequests", limitRequests.get());
        snapshot.put("resetTokensDuration", resetTokensDuration);
        snapshot.put("resetRequestsDuration", resetRequestsDuration);
        snapshot.put("lastHeaderUpdate", lastHeaderUpdate != null ? lastHeaderUpdate.toString() : "Not yet received");
        snapshot.put("trackedUsers", userStates.size());
        return snapshot;
    }
}
