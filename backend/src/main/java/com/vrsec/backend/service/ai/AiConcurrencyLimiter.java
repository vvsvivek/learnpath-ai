package com.vrsec.backend.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrency limiter and bounded request queue.
 * Bounds parallel in-flight calls to external AI providers to prevent TPM starvation.
 */
@Service
public class AiConcurrencyLimiter {

    private final Semaphore concurrencySemaphore;
    private final AtomicInteger inFlightCount = new AtomicInteger(0);
    private final AtomicInteger queuedCount = new AtomicInteger(0);

    private final int concurrencyLimit;
    private final int queueCapacity;
    private final long queueTimeoutMs;

    public AiConcurrencyLimiter(
            @Value("${groq.concurrency-limit:2}") int concurrencyLimit,
            @Value("${groq.queue-capacity:20}") int queueCapacity,
            @Value("${groq.queue-timeout-ms:15000}") long queueTimeoutMs) {
        this.concurrencyLimit = Math.max(1, concurrencyLimit);
        this.queueCapacity = Math.max(1, queueCapacity);
        this.queueTimeoutMs = Math.max(1000, queueTimeoutMs);
        this.concurrencySemaphore = new Semaphore(this.concurrencyLimit, true);
    }

    /**
     * Attempts to acquire an execution permit.
     * Enforces queue boundary and wait timeout.
     *
     * @throws AiServiceOverloadedException if queue capacity is exceeded.
     * @throws AiServiceTimeoutException    if timeout expires waiting in queue.
     */
    public void acquireSlot() throws AiServiceOverloadedException, AiServiceTimeoutException {
        // 1. Check queue bounds
        if (queuedCount.get() >= queueCapacity) {
            throw new AiServiceOverloadedException(
                    "The AI Tutor request queue is at full capacity (" + queueCapacity + " requests). Please retry in a moment.");
        }

        // 2. Increment queued counter and wait for semaphore permit
        queuedCount.incrementAndGet();
        try {
            boolean acquired = concurrencySemaphore.tryAcquire(queueTimeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new AiServiceTimeoutException(
                        "Timed out after " + (queueTimeoutMs / 1000) + "s waiting in the AI Tutor queue. Please retry.");
            }
            inFlightCount.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceTimeoutException("AI request was interrupted while waiting in queue.");
        } finally {
            queuedCount.decrementAndGet();
        }
    }

    /**
     * Releases an acquired concurrency permit.
     */
    public void releaseSlot() {
        inFlightCount.updateAndGet(curr -> Math.max(0, curr - 1));
        concurrencySemaphore.release();
    }

    public int getConcurrencyLimit() {
        return concurrencyLimit;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public int getInFlightCount() {
        return inFlightCount.get();
    }

    public int getQueuedCount() {
        return queuedCount.get();
    }

    public static class AiServiceOverloadedException extends RuntimeException {
        public AiServiceOverloadedException(String message) {
            super(message);
        }
    }

    public static class AiServiceTimeoutException extends RuntimeException {
        public AiServiceTimeoutException(String message) {
            super(message);
        }
    }
}
