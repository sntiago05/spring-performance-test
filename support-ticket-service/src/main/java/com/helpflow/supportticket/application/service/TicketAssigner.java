package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.port.out.TicketAssignmentPort;
import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.model.AssignmentSelection;
import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.TicketAssignment;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Applies an assignment: ticket state, assignment record and history entry. It always runs inside
 * the transaction of the calling use case, so the three writes succeed or fail together.
 */
@Component
class TicketAssigner {

    private final TicketRepositoryPort tickets;
    private final TicketAssignmentPort assignments;
    private final TicketHistoryPort history;
    private final TicketMetricsPort metrics;
    private final Clock clock;

    TicketAssigner(TicketRepositoryPort tickets, TicketAssignmentPort assignments, TicketHistoryPort history,
            TicketMetricsPort metrics, Clock clock) {
        this.tickets = tickets;
        this.assignments = assignments;
        this.history = history;
        this.metrics = metrics;
        this.clock = clock;
    }

    Ticket assign(Ticket ticket, AssignmentSelection selection, String assignedBy, HistoryAction action,
            String extraNotes) {
        Instant now = clock.instant();
        Ticket assigned = tickets.save(ticket.assignTo(selection.technician().id()));

        assignments.deactivateActiveOf(ticket.id());
        assignments.save(new TicketAssignment(null, ticket.id(), selection.technician().id(), selection.source(),
                selection.estimatedHours(), selection.availability(), selection.withinSla(), assignedBy, now,
                true));

        String estimate = selection.estimatedHours() == null ? ""
                : ", estimated " + selection.estimatedHours() + "h";
        String notes = "Assigned to " + selection.technician().code() + " (" + selection.source() + estimate + ")"
                + (extraNotes == null || extraNotes.isBlank() ? "" : ". " + extraNotes);
        history.append(new TicketHistoryEntry(null, ticket.id(), action, ticket.status(), assigned.status(),
                assignedBy, notes, now));
        metrics.ticketAssigned(selection.source());
        return assigned;
    }
}
