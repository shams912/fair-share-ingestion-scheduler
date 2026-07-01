package com.example.scheduler.strategy;

import com.example.scheduler.queue.TenantQueueRegistry;
import com.example.scheduler.registry.TenantStats;
import com.example.scheduler.registry.TenantStatsRegistry;
import com.example.scheduler.registry.TenantStatsSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wraps a single ScoringStrategy (dependency-injected via constructor, so it's
 * swappable at wiring time without touching this class) and handles the
 * mechanical part every strategy needs identically: build a snapshot per
 * candidate, take the argmax, and break ties the same deterministic way.
 */
public final class ScoringEngine {
    private final ScoringStrategy strategy;

    public ScoringEngine(ScoringStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * @param nonEmptyTenants tenants in this tier currently holding at least one message
     * @return the tenantId to dispatch next, or null if the candidate list was empty
     */
    public String selectNext(List<String> nonEmptyTenants, TenantStatsRegistry statsRegistry,
                             TenantQueueRegistry tenantQueueRegistry, long now) {
        // Sort first so iteration is lexical; combined with the strict "> bestScore"
        // check below, a tie keeps whichever tenantId sorts first alphabetically -
        // a deterministic tie-break with no round-robin bias, as the architecture requires.
        List<String> sortedCandidates = new ArrayList<>(nonEmptyTenants);
        Collections.sort(sortedCandidates);

        String best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (String tenantId : sortedCandidates) {
            TenantStats stats = statsRegistry.get(tenantId);
            TenantStatsSnapshot snapshot = new TenantStatsSnapshot(
                    stats, tenantQueueRegistry.queueFor(tenantId).size(), now);
            double score = strategy.score(tenantId, snapshot);
            if (score > bestScore) {
                bestScore = score;
                best = tenantId;
            }
        }
        return best;
    }
}
