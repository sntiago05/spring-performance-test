package com.helpflow.supportticket.infrastructure.config;

import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MetricsConfig {

    /**
     * Tickets whose SLA deadline already passed while still unresolved, evaluated on every scrape.
     */
    @Bean
    MeterBinder overdueTicketsGauge(TicketRepositoryPort tickets, Clock clock) {
        return (MeterRegistry registry) -> Gauge
                .builder("helpflow.tickets.overdue", () -> tickets.countOverdue(clock.instant()))
                .description("Unresolved tickets whose SLA deadline has already passed")
                .register(registry);
    }
}
