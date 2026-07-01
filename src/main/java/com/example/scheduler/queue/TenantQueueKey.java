package com.example.scheduler.queue;

import com.example.scheduler.config.TenantConfig;

public class TenantQueueKey {

    private final String tenantId;
    private final TenantConfig config;

    public TenantQueueKey(String tenantId, TenantConfig config) {
        this.tenantId = tenantId;
        this.config = config;
    }

    public String getTenantId() {
        return tenantId;
    }

    public TenantConfig getConfig() {
        return config;
    }
}
