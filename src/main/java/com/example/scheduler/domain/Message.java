package com.example.scheduler.domain;

import com.example.scheduler.queue.TenantQueueKey;

/**
 * Immutable value object - a plain data carrier with no behavior, so a record
 * fits better here than a class. (Contrast with a Hibernate entity: those need
 * mutability and a no-arg constructor for the persistence provider, so this
 * pattern wouldn't apply there - but for a message on a queue, it's a good fit.)
 */
public record Message(TenantQueueKey tenantQueueKey, int seq, long enqueueTimeMs) {
}
