package com.example.scheduler.queue;

import com.example.scheduler.domain.Tier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tier-namespaced collection of TenantQueues. Tiers are fully isolated here -
 * a GOLD tenant's traffic never competes with a BRONZE tenant's, because each
 * tier gets its own TenantQueueRegistry instance and its own WorkerDispatcher thread.
 */
public final class TenantQueueRegistry {
    private final Tier tier;
    private final Map<String, TenantQueue> tenantQueues = new ConcurrentHashMap<>();

    public TenantQueueRegistry(Tier tier) {
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    public void addTenant(String tenantId, int capacity) {
        tenantQueues.put(tenantId, new TenantQueue(capacity));
    }

    public TenantQueue queueFor(String tenantId) {
        return tenantQueues.get(tenantId);
    }

    public boolean allEmpty() {
        for (TenantQueue queue : tenantQueues.values()) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Candidates the ScoringEngine is allowed to pick from on this tick. */
    public List<String> nonEmptyTenants() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, TenantQueue> entry : tenantQueues.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
