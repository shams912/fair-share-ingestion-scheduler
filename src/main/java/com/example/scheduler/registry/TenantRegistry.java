package com.example.scheduler.registry;

import com.example.scheduler.domain.Tier;
import java.util.HashMap;
import java.util.Map;

/**
 * Static config: which tier a tenant belongs to. Deliberately separate from
 * TenantStatsRegistry - this rarely changes (tenant onboarding/tier changes),
 * while TenantStatsRegistry churns on every processed message. Different
 * lifecycles, different classes.
 */
public final class TenantRegistry {
    private final Map<String, Tier> tenantToTier = new HashMap<>();

    public void register(String tenantId, Tier tier) {
        tenantToTier.put(tenantId, tier);
    }

    public Tier tierOf(String tenantId) {
        return tenantToTier.get(tenantId);
    }
}
