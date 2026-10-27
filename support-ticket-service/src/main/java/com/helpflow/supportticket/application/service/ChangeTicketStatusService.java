package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.port.in.ChangeTicketStatusUseCase;
import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.exception.ForbiddenOperationException;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import com.helpflow.supportticket.domain.model.TicketStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ChangeTicketStatusService implements ChangeTicketStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangeTicketStatusService.class);

    private final TicketRepositoryPort tickets;
    private final TicketHistoryPort history;
    private final TicketMetricsPort metrics;
    private final TicketAccess access;
    private final Clock clock;

    ChangeTicketStatusService(TicketRepositoryPort tickets, TicketHistoryPort history, TicketMetricsPort metrics,
            TicketAccess access, Clock clock) {
        this.tickets = tickets;
        this.history = history;
        this.metrics = metrics;
        this.access = access;
        this.clock = clock;
    }

    @Override
    public TicketView changeStatus(Actor actor, Long ticketId, TicketStatus target, String observations) {
        Ticket current = tickets.findById(ticketId).orElseThrow(() -> GetTicketService.notFound(ticketId));
        access.assertCanManage(actor, current);
        if (target == TicketStatus.CANCELADO && !actor.isAdmin()) {
            throw new ForbiddenOperationException("cancel-admin-only",
                    "Only admins can cancel tickets (actor " + actor.userId() + ")");
        }

        Instant now = clock.instant();
        Ticket updated = tickets.save(current.transitionTo(target, now));
        history.append(new TicketHistoryEntry(null, ticketId, HistoryAction.STATUS_CHANGED, current.status(),
                target, actor.email(), observations.trim(), now));

        if (target == TicketStatus.EN_PROGRESO && current.firstResponseAt() == null) {
            metrics.firstResponse(Duration.between(current.createdAt(), now));
        }
        if (target == TicketStatus.RESUELTO) {
            metrics.resolution(Duration.between(current.createdAt(), now));
            if (updated.slaBreached() && !current.slaBreached()) {
                metrics.slaBreached();
                history.append(new TicketHistoryEntry(null, ticketId, HistoryAction.SLA_BREACHED, target, target,
                        TicketHistoryEntry.SYSTEM, "Resolved after the SLA deadline " + current.slaDueAt(), now));
            }
        }
        log.info("Ticket {} moved {} -> {} by {}", ticketId, current.status(), target, actor.email());
        return tickets.findViewById(ticketId).orElseThrow();
    }
}
