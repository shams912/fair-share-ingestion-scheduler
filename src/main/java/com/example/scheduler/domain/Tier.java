package com.example.scheduler.domain;

/**
 * Service tiers. QueueSpaces are keyed by this - GOLD, SILVER, and BRONZE never
 * share a queue, a thread, or an execution slot with each other (isolation is
 * enforced structurally, not by convention).
 */
public enum Tier {
    GOLD, SILVER, BRONZE
}
