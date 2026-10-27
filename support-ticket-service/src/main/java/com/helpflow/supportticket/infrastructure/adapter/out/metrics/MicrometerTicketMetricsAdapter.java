package com.helpflow.supportticket.infrastructure.adapter.out.metrics;

import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.domain.model.AssignmentSource;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Publishes business metrics through Micrometer. In Prometheus they appear as, for example,
 * {@code helpflow_tickets_registered_total{category,priority}} ("created" is avoided on purpose: Prometheus
 * reserves the {@code _created} suffix and Micrometer would rename the series),
 * {@code helpflow_tickets_first_response_seconds_sum/_count} and
 * {@code helpflow_tickets_sla_breached_total}.
 */
@Component
class MicrometerTicketMetricsAdapter implements TicketMetricsPort {

    private final MeterRegistry registry;
    private final Timer firstResponseTimer;
    private final Timer resolutionTimer;
    private final Counter slaBreached;
    private final Counter escalated;
    private final Counter accessDenied;

    MicrometerTicketMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
        this.firstResponseTimer = Timer.builder("helpflow.tickets.first.response")
                .description("Time from ticket creation until a technician starts working on it")
                .register(registry);
        this.resolutionTimer = Timer.builder("helpflow.tickets.resolution")
                .description("Time from ticket creation until it is resolved")
                .register(registry);
        this.slaBreached = Counter.builder("helpflow.tickets.sla.breached")
                .description("Tickets that exceeded their SLA deadline")
                .register(registry);
        this.escalated = Counter.builder("helpflow.tickets.escalated")
                .description("Tickets automatically escalated because they were close to breaching the SLA")
                .register(registry);
        this.accessDenied = Counter.builder("helpflow.access.denied")
                .description("Requests rejected with 403 Forbidden")
                .register(registry);
        // Visible with value 0 from the start so dashboards and alerts have a series to evaluate.
        Counter.builder("helpflow.auth.failures").tag("reason", "bad_credentials").register(registry);
    }

    @Override
    public void ticketCreated(Category category, Priority priority) {
        registry.counter("helpflow.tickets.registered", "category", category.name(), "priority", priority.name())
                .increment();
    }

    @Override
    public void ticketAssigned(AssignmentSource source) {
        registry.counter("helpflow.tickets.assigned", "source", source.name()).increment();
    }

    @Override
    public void firstResponse(Duration sinceCreation) {
        firstResponseTimer.record(sinceCreation);
    }

    @Override
    public void resolution(Duration sinceCreation) {
        resolutionTimer.record(sinceCreation);
    }

    @Override
    public void slaBreached() {
        slaBreached.increment();
    }

    @Override
    public void ticketEscalated() {
        escalated.increment();
    }

    @Override
    public void authenticationFailed(String reason) {
        registry.counter("helpflow.auth.failures", "reason", reason).increment();
    }

    @Override
    public void accessDenied() {
        accessDenied.increment();
    }
}
