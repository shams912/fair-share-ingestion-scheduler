package com.example.scheduler.queue;

import com.example.scheduler.domain.Message;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Bounded FIFO buffer for a single tenant. Bounded so one noisy tenant can't
 * grow memory without limit within its own tier - backpressure is applied at
 * the tenant level, not the tier level.
 */
public final class TenantQueue {
    private final ArrayBlockingQueue<Message> queue;

    public TenantQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public Message dequeue() {
        return queue.poll();
    }

    public boolean enqueue(Message message) {
        return queue.offer(message);
    }
}
