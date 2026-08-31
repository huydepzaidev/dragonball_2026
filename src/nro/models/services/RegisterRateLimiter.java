package nro.models.services;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe rate limiter per IP address to prevent registration spam.
 * Automatically cleans up expired entries to avoid memory leaks.
 */
public final class RegisterRateLimiter {

    private static final RegisterRateLimiter INSTANCE = new RegisterRateLimiter();

    private static final int MAX_ATTEMPTS_PER_WINDOW = 3;
    private static final long WINDOW_MS = 30_000L; // 30 seconds sliding window
    private static final long MIN_COOLDOWN_MS = 2_000L; // 2 seconds between attempts
    private static final long ENTRY_EXPIRY_MS = 60_000L; // 1 minute stale threshold
    private static final long CLEANUP_INTERVAL_MS = 30_000L;

    private final ConcurrentHashMap<String, Deque<Long>> ipHistory = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();

    private RegisterRateLimiter() {
    }

    public static RegisterRateLimiter gI() {
        return INSTANCE;
    }

    /**
     * Checks and records a registration attempt for an IP.
     *
     * @param ip IP address of the client
     * @return true if permitted, false if rate limited
     */
    public boolean tryAcquire(String ip) {
        String cleanIp = (ip == null || ip.isBlank()) ? "0.0.0.0" : ip.trim();
        long now = System.currentTimeMillis();

        triggerCleanupIfNeeded(now);

        Deque<Long> queue = ipHistory.computeIfAbsent(cleanIp, k -> new ArrayDeque<>());
        synchronized (queue) {
            // Check minimum cooldown between consecutive attempts
            Long lastAttempt = queue.peekLast();
            if (lastAttempt != null && (now - lastAttempt) < MIN_COOLDOWN_MS) {
                return false;
            }

            // Evict attempts older than the rolling window
            while (!queue.isEmpty() && (now - queue.peekFirst()) > WINDOW_MS) {
                queue.pollFirst();
            }

            // Check max attempts within the rolling window
            if (queue.size() >= MAX_ATTEMPTS_PER_WINDOW) {
                return false;
            }

            queue.addLast(now);
            return true;
        }
    }

    /**
     * Resets rate limits for a specific IP (useful for testing).
     */
    public void reset(String ip) {
        if (ip != null) {
            ipHistory.remove(ip.trim());
        }
    }

    /**
     * Clears all recorded attempts (useful for testing).
     */
    public void clear() {
        ipHistory.clear();
    }

    private void triggerCleanupIfNeeded(long now) {
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            lastCleanup = now;
            // Clean up expired entries in a lightweight sweep
            for (Map.Entry<String, Deque<Long>> entry : ipHistory.entrySet()) {
                Deque<Long> queue = entry.getValue();
                synchronized (queue) {
                    while (!queue.isEmpty() && (now - queue.peekFirst()) > ENTRY_EXPIRY_MS) {
                        queue.pollFirst();
                    }
                    if (queue.isEmpty()) {
                        ipHistory.remove(entry.getKey(), queue);
                    }
                }
            }
        }
    }
}
