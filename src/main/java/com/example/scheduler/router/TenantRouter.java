package com.example.scheduler.router;

import com.example.scheduler.domain.Message;
import com.example.scheduler.domain.Tier;
import com.example.scheduler.queue.TenantQueueRegistry;
import com.example.scheduler.queue.TenantQueue;
import com.example.scheduler.registry.TenantRegistry;
import java.util.Map;

/**
 * Layer 1 of the architecture. Stands in for "poll Kafka, resolve tenant, enqueue"
 * in this simulation - looks up the tenant's tier via TenantRegistry and drops
 * the message into that tier's QueueSpace. A real Kafka consumer loop would call
 * route() once per polled record instead of the batch-preload loop this sim uses.
 */
public final class TenantRouter {
    private final TenantRegistry tenantRegistry;
    private final Map<Tier, TenantQueueRegistry> queueSpaces;

    public TenantRouter(TenantRegistry tenantRegistry, Map<Tier, TenantQueueRegistry> queueSpaces) {
        this.tenantRegistry = tenantRegistry;
        this.queueSpaces = queueSpaces;
    }

    public void route(Message message) {
        Tier tier = tenantRegistry.tierOf(message.tenantId());
        TenantQueue tenantQueue = queueSpaces.get(tier).queueFor(message.tenantId());
        tenantQueue.enqueue(message);
    }
}
