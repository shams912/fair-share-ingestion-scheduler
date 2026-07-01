package com.example.scheduler.registry;

/**
 * Immutable snapshot handed to a ScoringStrategy. Strategies never see the live,
 * mutable TenantStats object - only a frozen copy of what mattered at decision
 * time. This keeps strategies pure functions of (tenantId, snapshot), which
 * makes them trivial to unit test in isolation later (Step 4).
 */
public final class TenantStatsSnapshot {
    private final long lastProcessedAt;
    private final int queueDepth;
    private final int allocatedCapacity;
    private final long now;

    public TenantStatsSnapshot(TenantStats stats, int queueDepth, long now) {
        this.lastProcessedAt = stats.lastProcessedAt();
        this.queueDepth = queueDepth;
        this.allocatedCapacity = stats.allocatedCapacity();
        this.now = now;
    }

    public long lastProcessedAt() {
        return lastProcessedAt;
    }

    public int queueDepth() {
        return queueDepth;
    }

    public int allocatedCapacity() {
        return allocatedCapacity;
    }

    public long now() {
        return now;
    }
}
