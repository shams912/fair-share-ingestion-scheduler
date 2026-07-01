package com.example.scheduler.dispatcher;

import com.example.scheduler.domain.Message;
import com.example.scheduler.processor.MessageProcessor;
import com.example.scheduler.queue.TenantQueueRegistry;
import com.example.scheduler.queue.TenantQueue;
import com.example.scheduler.registry.TenantStats;
import com.example.scheduler.registry.TenantStatsRegistry;
import com.example.scheduler.strategy.ScoringEngine;
import java.util.List;

/**
 * Layer 4's main loop. One WorkerDispatcher runs per QueueSpace/tier, on its own
 * thread. Because each instance only ever touches tenants belonging to its own
 * tier, there's no cross-thread contention on TenantStats - no locks needed
 * here beyond what the JDK's queues already provide.
 *
 * This class deliberately knows nothing about *how* to score a tenant (that's
 * ScoringEngine's job) or *how* to process a message (that's MessageProcessor's
 * job) - it only knows the sequence: pick, dispatch, process, record, repeat.
 * That separation is what makes strategies and processors swappable without
 * ever touching this file.
 */
public final class WorkerDispatcher implements Runnable {
    private final TenantQueueRegistry tenantQueueRegistry;
    private final ScoringEngine scoringEngine;
    private final TenantStatsRegistry statsRegistry;

    public WorkerDispatcher(TenantQueueRegistry tenantQueueRegistry, ScoringEngine scoringEngine, TenantStatsRegistry statsRegistry) {
        this.tenantQueueRegistry = tenantQueueRegistry;
        this.scoringEngine = scoringEngine;
        this.statsRegistry = statsRegistry;
    }

    @Override
    public void run() {
        // Batch-preload simulation: no producer adds messages once this loop starts,
        // so allEmpty() is a safe terminating condition - no race with a live producer.
        // A real Kafka-backed version would replace this with "poll forever".
        while (!tenantQueueRegistry.allEmpty()) {
            List<String> candidates = tenantQueueRegistry.nonEmptyTenants();
            long now = System.currentTimeMillis();
            String tenantId = scoringEngine.selectNext(candidates, statsRegistry, tenantQueueRegistry, now);

            // Execution Slot: "one active tenant per QueueSpace" is enforced simply by
            // this thread processing one message at a time, inline, before looping back.
            TenantQueue tenantQueue = tenantQueueRegistry.queueFor(tenantId);
            Message message = tenantQueue.dequeue();
            if (message == null) {
                continue; // defensive only - shouldn't happen given single-threaded access per tier
            }

            long dispatchTime = System.currentTimeMillis();
            long waitTimeMs = dispatchTime - message.enqueueTimeMs();

            // Blocking call - see MessageProcessor's javadoc for why this doesn't cost concurrency.
            long execTimeMs = MessageProcessor.process(message);

            TenantStats stats = statsRegistry.get(tenantId);
            stats.recordProcessing(dispatchTime, waitTimeMs, execTimeMs);

            // TODO: Commenting this log until metrics logic is implemented
            /* System.out.printf("[%-6s] tenant=%-4s seq=%-3d waitMs=%-5d execMs=%-4d queueDepthAfter=%d%n",
                    tenantQueueRegistry.tier(), tenantId, message.seq(), waitTimeMs, execTimeMs, tenantQueue.size());
             */
        }
        System.out.println("[" + tenantQueueRegistry.tier() + "] dispatcher finished - queues drained.");
    }
}
