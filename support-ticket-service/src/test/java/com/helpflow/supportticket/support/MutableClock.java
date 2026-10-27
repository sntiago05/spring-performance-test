package com.helpflow.supportticket.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Test clock that can be moved forward to exercise SLA escalation without waiting.
 */
public class MutableClock extends Clock {

    private Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

    public synchronized void advance(Duration duration) {
        now = now.plus(duration);
    }

    public synchronized void reset() {
        now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public synchronized Instant instant() {
        return now;
    }
}
