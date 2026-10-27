package com.helpflow.supportticket.infrastructure.adapter.in.scheduler;

import com.helpflow.supportticket.application.port.in.EscalateTicketsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic trigger of the SLA review. It is a driving adapter like a controller: it only calls the
 * use case. Single-instance by design; with several replicas a distributed lock (e.g. ShedLock)
 * would be needed to avoid duplicate runs.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.sla.review-enabled", havingValue = "true", matchIfMissing = true)
class SlaEscalationJob {

    private final EscalateTicketsUseCase escalateTickets;

    SlaEscalationJob(EscalateTicketsUseCase escalateTickets) {
        this.escalateTickets = escalateTickets;
    }

    @Scheduled(fixedDelayString = "${app.sla.review-interval:PT1M}", initialDelayString = "${app.sla.review-initial-delay:PT30S}")
    void run() {
        escalateTickets.escalateDueTickets();
    }
}
