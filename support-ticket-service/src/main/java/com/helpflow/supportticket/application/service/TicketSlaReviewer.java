package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reviews one ticket against its SLA inside its own transaction.
 */
@Component
class TicketSlaReviewer {

    private static final Logger log = LoggerFactory.getLogger(TicketSlaReviewer.class);

    record Outcome(boolean escalated, boolean breached) {
    }

    private final TicketRepositoryPort tickets;
    private final TicketHistoryPort history;
    private final SlaPolicyRepositoryPort slaPolicies;
    private final TicketMetricsPort metrics;
    private final Clock clock;

    TicketSlaReviewer(TicketRepositoryPort tickets, TicketHistoryPort history, SlaPolicyRepositoryPort slaPolicies,
            TicketMetricsPort metrics, Clock clock) {
        this.tickets = tickets;
        this.history = history;
        this.slaPolicies = slaPolicies;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    Outcome review(Long ticketId) {
        Ticket ticket = tickets.findById(ticketId).orElse(null);
        if (ticket == null || !ticket.status().isSlaRunning()) {
            return new Outcome(false, false);
        }
        Instant now = clock.instant();
        boolean escalated = false;
        boolean breached = false;

        int threshold = slaPolicies.findByPriority(ticket.priority()).orElseThrow().escalationThresholdPercent();
        if (ticket.needsEscalation(now, threshold)) {
            Priority before = ticket.priority();
            ticket = tickets.save(ticket.escalate(now));
            history.append(new TicketHistoryEntry(null, ticketId, HistoryAction.ESCALATED, ticket.status(),
                    ticket.status(), TicketHistoryEntry.SYSTEM,
                    "Auto-escalated at %d%% of the SLA window (priority %s -> %s)".formatted(
                            threshold, before, ticket.priority()), now));
            metrics.ticketEscalated();
            escalated = true;
            log.warn("Ticket {} escalated: priority {} -> {}", ticketId, before, ticket.priority());
        }
        if (!ticket.slaBreached() && ticket.isOverdue(now)) {
            ticket = tickets.save(ticket.markSlaBreached());
            history.append(new TicketHistoryEntry(null, ticketId, HistoryAction.SLA_BREACHED, ticket.status(),
                    ticket.status(), TicketHistoryEntry.SYSTEM, "SLA deadline " + ticket.slaDueAt() + " exceeded",
                    now));
            metrics.slaBreached();
            breached = true;
            log.warn("Ticket {} breached its SLA deadline {}", ticketId, ticket.slaDueAt());
        }
        return new Outcome(escalated, breached);
    }
}
