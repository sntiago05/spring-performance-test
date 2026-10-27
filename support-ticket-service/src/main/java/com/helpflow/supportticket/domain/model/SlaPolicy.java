package com.helpflow.supportticket.domain.model;

import java.time.Duration;
import java.time.Instant;

/**
 * SLA configuration per priority: resolution window and the share of that window after which an
 * unresolved ticket is considered close to breaching and gets escalated.
 */
public record SlaPolicy(Priority priority, int resolutionHours, int escalationThresholdPercent) {

    public SlaPolicy {
        if (resolutionHours < 1) {
            throw new IllegalArgumentException("resolutionHours must be at least 1");
        }
        if (escalationThresholdPercent < 1 || escalationThresholdPercent > 99) {
            throw new IllegalArgumentException("escalationThresholdPercent must be between 1 and 99");
        }
    }

    public Instant dueAt(Instant createdAt) {
        return createdAt.plus(Duration.ofHours(resolutionHours));
    }
}
