package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.port.in.EscalateTicketsUseCase;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Drives the periodic SLA review. Each ticket is processed in its own transaction (see
 * {@link TicketSlaReviewer}) so one failing ticket does not roll back the others.
 */
@Service
class EscalateTicketsService implements EscalateTicketsUseCase {

    private static final Logger log = LoggerFactory.getLogger(EscalateTicketsService.class);

    private final TicketRepositoryPort tickets;
    private final TicketSlaReviewer reviewer;

    EscalateTicketsService(TicketRepositoryPort tickets, TicketSlaReviewer reviewer) {
        this.tickets = tickets;
        this.reviewer = reviewer;
    }

    @Override
    public Summary escalateDueTickets() {
        var ids = tickets.findIdsPendingSlaReview();
        int escalated = 0;
        int breached = 0;
        int failed = 0;
        for (Long id : ids) {
            try {
                TicketSlaReviewer.Outcome outcome = reviewer.review(id);
                escalated += outcome.escalated() ? 1 : 0;
                breached += outcome.breached() ? 1 : 0;
            } catch (RuntimeException ex) {
                failed++;
                log.error("SLA review failed for ticket {}", id, ex);
            }
        }
        if (escalated > 0 || breached > 0 || failed > 0) {
            log.info("SLA review: {} reviewed, {} escalated, {} breached, {} failed", ids.size(), escalated,
                    breached, failed);
        }
        return new Summary(ids.size(), escalated, breached, failed);
    }
}
