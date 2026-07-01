package com.example.scheduler.app;

import com.example.scheduler.domain.Message;
import com.example.scheduler.domain.Tier;
import com.example.scheduler.dispatcher.WorkerDispatcher;
import com.example.scheduler.queue.TenantQueueRegistry;
import com.example.scheduler.registry.TenantRegistry;
import com.example.scheduler.registry.TenantStats;
import com.example.scheduler.registry.TenantStatsRegistry;
import com.example.scheduler.router.TenantRouter;
import com.example.scheduler.strategy.LRUScoringStrategy;
import com.example.scheduler.strategy.ScoringEngine;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Composition root - the one class allowed to know about every layer, because
 * its only job is wiring them together. If this were Spring, this is the
 * @Configuration/@Bean setup; there's no container here, so wiring happens by
 * hand in main() instead of via component scanning.
 */
public final class SchedulerSimulationApp {

    /** Static demo config: tenantId, tier, allocated capacity, message count to preload. */
    private record TenantConfig(String tenantId, Tier tier, int allocatedCapacity, int messageCount) {
    }

    private static final TenantConfig[] TENANT_CONFIGS = {
            new TenantConfig("G1", Tier.GOLD, 100, 500),
            new TenantConfig("G2", Tier.GOLD, 100, 30),
            new TenantConfig("S1", Tier.SILVER, 60, 14),
            new TenantConfig("S2", Tier.SILVER, 60, 60),
            new TenantConfig("S3", Tier.SILVER, 60, 20),
            new TenantConfig("B1", Tier.BRONZE, 30, 8),
            new TenantConfig("B2", Tier.BRONZE, 30, 2),
    };
    private static final int PER_TENANT_QUEUE_CAPACITY = 50;

    public static void main(String[] args) throws InterruptedException {
        TenantRegistry tenantRegistry = new TenantRegistry();
        TenantStatsRegistry statsRegistry = new TenantStatsRegistry();
        Map<Tier, TenantQueueRegistry> queueSpaces = new EnumMap<>(Tier.class);
        for (Tier tier : Tier.values()) {
            queueSpaces.put(tier, new TenantQueueRegistry(tier));
        }

        for (TenantConfig config : TENANT_CONFIGS) {
            tenantRegistry.register(config.tenantId(), config.tier());
            statsRegistry.register(config.tenantId(), config.allocatedCapacity());
            queueSpaces.get(config.tier()).addTenant(config.tenantId(), config.messageCount());
        }

        TenantRouter router = new TenantRouter(tenantRegistry, queueSpaces);

        // Batch preload: every message exists before any WorkerDispatcher thread starts.
        long t0 = System.currentTimeMillis();
        for (TenantConfig config : TENANT_CONFIGS) {
            for (int seq = 1; seq <= config.messageCount(); seq++) {
                router.route(new Message(config.tenantId(), seq, t0));
            }
        }

        runWorkers(queueSpaces, statsRegistry);
        printSummary(statsRegistry);
    }

    /** One WorkerDispatcher thread per tier - tiers never share a thread or an execution slot. */
    private static void runWorkers(Map<Tier, TenantQueueRegistry> queueSpaces,
                                   TenantStatsRegistry statsRegistry) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(Tier.values().length);
        for (Tier tier : Tier.values()) {
            // LRU is the only strategy wired up today; swapping this one line for another
            // ScoringStrategy implementation is the entire cost of changing algorithms -
            // nothing else in the app needs to change.
            ScoringEngine scoringEngine = new ScoringEngine(new LRUScoringStrategy());
            pool.submit(new WorkerDispatcher(queueSpaces.get(tier), scoringEngine, statsRegistry));
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.MINUTES);
    }

    private static void printSummary(TenantStatsRegistry statsRegistry) {
        System.out.println("\n=== Summary ===");
        for (TenantConfig config : TENANT_CONFIGS) {
            TenantStats stats = statsRegistry.get(config.tenantId());
            System.out.printf("%-4s processed=%-3d avgWaitMs=%-8.1f avgExecMs=%-8.1f%n",
                    config.tenantId(), stats.messagesProcessedCount(),
                    stats.avgWaitTimeMs(), stats.avgExecTimePerMessage());
        }
    }
}
