package com.example.scheduler.strategy;

import com.example.scheduler.registry.TenantStatsSnapshot;

/**
 * Least Recently Used: the tenant idle the longest scores highest.
 *
 * Tenants that have never been processed have lastProcessedAt = 0, which makes
 * (now - 0) a very large number - so brand-new tenants naturally win their first
 * turn without any special-casing in this class.
 *
 * If this strategy needed explicit starvation protection (an aging term beyond
 * plain LRU), it would be added here, not in the WorkerDispatcher - per the
 * architecture decision that keeps starvation handling inside individual
 * strategies so the WorkerDispatcher stays Open/Closed.
 */
public final class LRUScoringStrategy implements ScoringStrategy {
    @Override
    public double score(String tenantId, TenantStatsSnapshot snapshot) {
        return snapshot.now() - snapshot.lastProcessedAt();
    }
}
