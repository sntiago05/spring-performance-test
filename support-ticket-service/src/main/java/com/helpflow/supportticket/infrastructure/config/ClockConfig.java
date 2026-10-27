package com.helpflow.supportticket.infrastructure.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfig {

    /**
     * UTC clock truncated to microseconds, the precision PostgreSQL stores: what the API returns right
     * after a write is then identical to what it returns when the row is read back later.
     */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
