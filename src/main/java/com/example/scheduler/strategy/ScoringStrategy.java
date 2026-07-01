package com.example.scheduler.strategy;

import com.example.scheduler.registry.TenantStatsSnapshot;

/**
 * Pluggable scoring contract (Strategy pattern). The WorkerDispatcher and
 * ScoringEngine never need to change when a new strategy is added - only a new
 * class implementing this interface, plus a config switch. This is the
 * Open/Closed boundary from the architecture doc: scheduling.strategy =
 * LRU | WRR | CAPACITY | QUEUE_SIZE all plug in here without touching anything
 * upstream.
 */
public interface ScoringStrategy {
    /**
     * Higher score = higher priority to run next. Only ever called for tenants
     * that currently have at least one message queued.
     */
    double score(String tenantId, TenantStatsSnapshot snapshot);
}
