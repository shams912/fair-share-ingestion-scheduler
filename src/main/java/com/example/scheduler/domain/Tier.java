package com.example.scheduler.domain;

/**
 * Service tiers along with their weights. TenantQueueRegistries are keyed by this - GOLD, SILVER, and BRONZE never
 * share a queue, a thread, or an execution slot with each other (isolation is
 * enforced structurally, not by convention).
 */
public enum Tier {
    GOLD(3), SILVER(2), BRONZE(1);

    private final int weight;

    Tier(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
