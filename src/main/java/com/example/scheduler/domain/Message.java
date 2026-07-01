package com.example.scheduler.domain;

/**
 * Immutable value object - a plain data carrier with no behavior, so a record
 * fits better here than a class. (Contrast with a Hibernate entity: those need
 * mutability and a no-arg constructor for the persistence provider, so this
 * pattern wouldn't apply there - but for a message on a queue, it's a good fit.)
 */
public record Message(String tenantId, int seq, long enqueueTimeMs) {
}
