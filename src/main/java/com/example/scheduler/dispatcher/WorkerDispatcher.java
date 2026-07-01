package com.example.scheduler.dispatcher;

import com.example.scheduler.config.TenantConfig;
import com.example.scheduler.domain.Message;
import com.example.scheduler.domain.Tier;
import com.example.scheduler.processor.MessageProcessor;
import com.example.scheduler.queue.TenantQueueKey;
import com.example.scheduler.queue.TenantQueueRegistry;
import com.example.scheduler.queue.TenantQueue;
import com.example.scheduler.registry.TenantStats;
import com.example.scheduler.registry.TenantStatsRegistry;
import com.example.scheduler.strategy.ScoringEngine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
    private static final int FLUSH_INTERVAL = 100;
    private final TenantQueueRegistry tenantQueueRegistry;
    private final ScoringEngine scoringEngine;
    private final TenantStatsRegistry statsRegistry;
    private final BufferedWriter writer;
    private final Tier tier;

    public WorkerDispatcher(TenantQueueRegistry tenantQueueRegistry, ScoringEngine scoringEngine, TenantStatsRegistry statsRegistry) throws IOException {
        this.tenantQueueRegistry = tenantQueueRegistry;
        this.scoringEngine = scoringEngine;
        this.statsRegistry = statsRegistry;
        this.tier = tenantQueueRegistry.tier();

        Path csvFile = Path.of("metrics-" + tier + ".csv");
        writer =
                Files.newBufferedWriter(
                        csvFile,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
        writer.write("timestamp,tier,tenant,seq,waitMs,execMs,queueDepth");
        writer.newLine();
    }

    @Override
    public void run() {
        // Exit when:
        // 1. all queues are empty, OR
        // 2. batch size is reached, OR
        // 3. batch time is exceeded
        try {
            while (!tenantQueueRegistry.allEmpty()) {
                long now = System.currentTimeMillis();
                TenantQueueKey tenantQueueKey = scoringEngine.selectNext(
                        tenantQueueRegistry.nonEmptyTenants(),
                        statsRegistry,
                        tenantQueueRegistry,
                        now);

                String tenantId = tenantQueueKey.getTenantId();
                TenantQueue tenantQueue = tenantQueueRegistry.queueFor(tenantId);
                TenantStats stats = statsRegistry.get(tenantId);
                TenantConfig config = tenantQueueKey.getConfig();

                int processedCount = 0;
                long deadline = System.currentTimeMillis() + config.getBatchTimeInMS();
                int batchSize = config.getBatchSize();

                while (processedCount < batchSize
                        && System.currentTimeMillis() < deadline) {

                    Message message = tenantQueue.dequeue();
                    if (message == null) {
                        break;
                    }

                    long dispatchTime = System.currentTimeMillis();
                    long waitTimeMs = dispatchTime - message.enqueueTimeMs();

                    // Blocking call - see MessageProcessor's javadoc for why this doesn't cost concurrency.
                    long execTimeMs = MessageProcessor.process(message);


                    stats.recordProcessing(dispatchTime, waitTimeMs, execTimeMs, tenantQueue.size());

                    processedCount++;

                    writeMetric(
                            dispatchTime,
                            tenantId,
                            message.seq(),
                            waitTimeMs,
                            execTimeMs,
                            tenantQueue.size());

                    // TODO: Commenting this log until metrics logic is implemented
            /* System.out.printf("[%-6s] tenant=%-4s seq=%-3d waitMs=%-5d execMs=%-4d queueDepthAfter=%d%n",
                    tenantQueueRegistry.tier(), tenantId, message.seq(), waitTimeMs, execTimeMs, tenantQueue.size());
             */
                }
            }
            System.out.println("[" + tenantQueueRegistry.tier() + "] dispatcher finished - queues drained.");

        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing metrics.", e);
        } finally {
            closeWriter();
        }
    }

    private void writeMetric(
            long timestamp,
            String tenantId,
            int sequence,
            long waitTimeMs,
            long execTimeMs,
            int queueDepth) throws IOException {

        writer.write(
                timestamp + "," +
                        tier + "," +
                        tenantId + "," +
                        sequence + "," +
                        waitTimeMs + "," +
                        execTimeMs + "," +
                        queueDepth);

        writer.newLine();
    }

    private void closeWriter() {

        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close metrics writer.", e);
        }
    }
}
