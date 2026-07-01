package com.example.scheduler.processor;

import com.example.scheduler.domain.Message;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Layer 5. Deliberately stateless - it has no idea about tenants, tiers, or
 * scheduling, only how to do the work for one message and report how long that
 * took. Modeled as a static utility (private constructor, no instance fields)
 * since there's genuinely no state to hold.
 */
public final class MessageProcessor {

    private MessageProcessor() {
        // no instances - stateless utility
    }

    /**
     * Simulates the work for one message. Blocking by design: the WorkerDispatcher's
     * execution slot is already single-threaded per tier, so a callback-based
     * processor would add complexity without buying any extra concurrency -
     * this resolves the open question from the architecture doc.
     *
     * @return execution time in ms, reported back to the WorkerDispatcher for stats
     */
    public static long process(Message message) {
        long execTimeMs = ThreadLocalRandom.current().nextLong(15, 60);
        try {
            Thread.sleep(execTimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return execTimeMs;
    }
}
