package com.example.scheduler.config;

import com.example.scheduler.domain.Tier;

public class TenantConfig {

    private final String tenantId;
    private final Tier tier;
    private final int priority;
    private final int messageCount;

    public TenantConfig(String tenantId, Tier tier, int priority, int messageCount) {
        this.tenantId = tenantId;
        this.tier = tier;
        this.priority = priority;
        this.messageCount = messageCount;
    }

    public String tenantId() {
        return tenantId;
    }

    public Tier tier() {
        return tier;
    }

    public int messageCount() {
        return messageCount;
    }

    public int getAllocatedCapacity() {
        return priority * tier.getWeight();
    }

    public int getBatchTimeInMS() {
        int defaultBatchTimeInMS = 1000;
        return defaultBatchTimeInMS * getAllocatedCapacity();
    }

    public int getBatchSize() {
        int defaultBatchSize = 20;
        return defaultBatchSize * getAllocatedCapacity();
    }
}
