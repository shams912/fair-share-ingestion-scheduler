package com.example.scheduler.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared state layer from the architecture doc. ConcurrentHashMap only protects
 * the map's own structure - all registrations happen before any WorkerDispatcher
 * thread starts, and after that each entry has exactly one writer (see
 * TenantStats' thread-safety note), so no additional locking is needed here.
 */
public final class TenantStatsRegistry {
    private final Map<String, TenantStats> stats = new ConcurrentHashMap<>();

    public void register(String tenantId, int allocatedCapacity) {
        stats.put(tenantId, new TenantStats(allocatedCapacity));
    }

    public TenantStats get(String tenantId) {
        return stats.get(tenantId);
    }
}
