package com.example.scheduler.app;

import com.example.scheduler.config.TenantConfig;
import com.example.scheduler.domain.Message;
import com.example.scheduler.domain.Tier;
import com.example.scheduler.dispatcher.WorkerDispatcher;
import com.example.scheduler.metrics.MetricsChartGenerator;
import com.example.scheduler.queue.TenantQueueKey;
import com.example.scheduler.queue.TenantQueueRegistry;
import com.example.scheduler.registry.TenantRegistry;
import com.example.scheduler.registry.TenantStats;
import com.example.scheduler.registry.TenantStatsRegistry;
import com.example.scheduler.router.TenantRouter;
import com.example.scheduler.strategy.LRUScoringStrategy;
import com.example.scheduler.strategy.ScoringEngine;

import java.io.IOException;
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

    private static final TenantConfig[] TENANT_CONFIGS = {
            new TenantConfig("G1", Tier.GOLD, 1, 500),
            new TenantConfig("G2", Tier.GOLD, 1, 300),
            new TenantConfig("S1", Tier.SILVER, 1, 14),
            new TenantConfig("S2", Tier.SILVER, 1, 60),
            new TenantConfig("S3", Tier.SILVER, 1, 20),
            new TenantConfig("B1", Tier.BRONZE, 1, 8),
            new TenantConfig("B2", Tier.BRONZE, 1, 2),
    };
    private static final int PER_TENANT_QUEUE_CAPACITY = 50;

    public static void main(String[] args) throws InterruptedException, IOException {
        TenantRegistry tenantRegistry = new TenantRegistry();
        TenantStatsRegistry statsRegistry = new TenantStatsRegistry();
        Map<Tier, TenantQueueRegistry> tenantQueueRegistryMap = new EnumMap<>(Tier.class);
        for (Tier tier : Tier.values()) {
            tenantQueueRegistryMap.put(tier, new TenantQueueRegistry(tier));
        }

        for (TenantConfig config : TENANT_CONFIGS) {
            TenantQueueKey tenantQueueKey = new TenantQueueKey(config.tenantId(), config);
            tenantRegistry.register(tenantQueueKey, config.tier());
            statsRegistry.register(config.tenantId(), config.getAllocatedCapacity());
            tenantQueueRegistryMap.get(config.tier()).addTenant(tenantQueueKey, config.messageCount());
        }

        TenantRouter router = new TenantRouter(tenantRegistry, tenantQueueRegistryMap);

        // Batch preload: every message exists before any WorkerDispatcher thread starts.
        long t0 = System.currentTimeMillis();
        for (TenantConfig config : TENANT_CONFIGS) {
            for (int seq = 1; seq <= config.messageCount(); seq++) {
                TenantQueueKey tenantQueueKey = new TenantQueueKey(config.tenantId(), config);
                router.route(new Message(tenantQueueKey, seq, t0));
            }
        }

        runWorkers(tenantQueueRegistryMap, statsRegistry);
        printSummary(statsRegistry);
    }

    /** One WorkerDispatcher thread per tier - tiers never share a thread or an execution slot. */
    private static void runWorkers(Map<Tier, TenantQueueRegistry> tenantQueueRegistryMap,
                                   TenantStatsRegistry statsRegistry) throws InterruptedException, IOException {
        ExecutorService pool = Executors.newFixedThreadPool(Tier.values().length);
        for (Tier tier : Tier.values()) {
            // LRU is the only strategy wired up today; swapping this one line for another
            // ScoringStrategy implementation is the entire cost of changing algorithms -
            // nothing else in the app needs to change.
            ScoringEngine scoringEngine = new ScoringEngine(new LRUScoringStrategy());
            pool.submit(new WorkerDispatcher(tenantQueueRegistryMap.get(tier), scoringEngine, statsRegistry));
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.MINUTES);
    }

    private static void printSummary(TenantStatsRegistry statsRegistry) throws IOException {
        System.out.println("\n=== Summary ===");
        for (TenantConfig config : TENANT_CONFIGS) {
            TenantStats stats = statsRegistry.get(config.tenantId());
            System.out.printf("%-4s processed=%-8d avgWaitMs=%-8.1f avgExecMs=%-8.1f lastExecTimeMs=%-9d%n",
                    config.tenantId(), stats.messagesProcessedCount(),
                    stats.avgWaitTimeMs(), stats.avgExecTimePerMessage(),
                    stats.lastProcessedAt());
        }
        MetricsChartGenerator.generateCharts("metrics-GOLD.csv", Tier.GOLD.name());
        MetricsChartGenerator.generateCharts("metrics-SILVER.csv", Tier.SILVER.name());
        MetricsChartGenerator.generateCharts("metrics-BRONZE.csv", Tier.BRONZE.name());
    }
}
