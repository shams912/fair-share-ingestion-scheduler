package com.example.scheduler.registry;

/**
 * Per-tenant running stats. recordProcessing() is the only mutator - the
 * WorkerDispatcher reports an outcome, this class decides how to fold it into its
 * own state, instead of the WorkerDispatcher reaching in and setting fields directly.
 *
 * Thread-safety note: exactly one WorkerDispatcher thread ever calls recordProcessing()
 * for a given tenant (a tenant belongs to exactly one tier, one tier = one thread),
 * so there's no write contention. Fields are volatile only so the end-of-run report
 * (read from the main thread after the pool joins) sees the latest values.
 */
public final class TenantStats {
    private volatile long lastProcessedAt = 0L; // 0 => never processed => scores highest under LRU
    private volatile long totalWaitTimeMs = 0L;
    private volatile long totalExecTimeMs = 0L;
    private volatile int messagesProcessedCount = 0;
    private final int allocatedCapacity;

    public TenantStats(int allocatedCapacity) {
        this.allocatedCapacity = allocatedCapacity;
    }

    public void recordProcessing(long dispatchTime, long waitTimeMs, long execTimeMs) {
        this.lastProcessedAt = dispatchTime;
        this.totalWaitTimeMs += waitTimeMs;
        this.totalExecTimeMs += execTimeMs;
        this.messagesProcessedCount++;
    }

    public long lastProcessedAt() {
        return lastProcessedAt;
    }

    public int allocatedCapacity() {
        return allocatedCapacity;
    }

    public int messagesProcessedCount() {
        return messagesProcessedCount;
    }

    public double avgExecTimePerMessage() {
        return messagesProcessedCount == 0 ? 0.0 : (double) totalExecTimeMs / messagesProcessedCount;
    }

    public double avgWaitTimeMs() {
        return messagesProcessedCount == 0 ? 0.0 : (double) totalWaitTimeMs / messagesProcessedCount;
    }
}
