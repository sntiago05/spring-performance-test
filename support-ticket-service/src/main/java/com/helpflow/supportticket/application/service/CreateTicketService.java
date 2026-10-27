package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.port.in.CreateTicketUseCase;
import com.helpflow.supportticket.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.supportticket.application.port.out.TechnicianDispatchPort;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.application.port.out.UserRepositoryPort;
import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.ResourceNotFoundException;
import com.helpflow.supportticket.domain.exception.TechnicianDispatchUnavailableException;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.AssignmentSelection;
import com.helpflow.supportticket.domain.model.AssignmentSource;
import com.helpflow.supportticket.domain.model.DispatchSuggestion;
import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.SlaPolicy;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.domain.service.TicketAssignmentPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and initial assignment run in a single transaction: the ticket, its first history
 * entry, the assignment and the assignment history entry are stored together or not at all. The
 * dispatch call is bounded by short HTTP timeouts and, when it fails, the internal rules still assign.
 */
@Service
@Transactional
class CreateTicketService implements CreateTicketUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateTicketService.class);

    private final UserRepositoryPort users;
    private final TicketRepositoryPort tickets;
    private final TicketHistoryPort history;
    private final SlaPolicyRepositoryPort slaPolicies;
    private final TechnicianRepositoryPort technicians;
    private final TechnicianDispatchPort dispatch;
    private final TicketAssigner assigner;
    private final TicketMetricsPort metrics;
    private final TicketAssignmentPolicy assignmentPolicy = new TicketAssignmentPolicy();
    private final Clock clock;

    CreateTicketService(UserRepositoryPort users, TicketRepositoryPort tickets, TicketHistoryPort history,
            SlaPolicyRepositoryPort slaPolicies, TechnicianRepositoryPort technicians,
            TechnicianDispatchPort dispatch, TicketAssigner assigner, TicketMetricsPort metrics, Clock clock) {
        this.users = users;
        this.tickets = tickets;
        this.history = history;
        this.slaPolicies = slaPolicies;
        this.technicians = technicians;
        this.dispatch = dispatch;
        this.assigner = assigner;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public TicketView create(Actor actor, Command command) {
        User requester = users.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("user-not-found",
                        "User " + actor.userId() + " not found", actor.userId()));
        if (!requester.isActive()) {
            throw new BusinessRuleViolationException("user-inactive",
                    "Inactive user " + requester.id() + " cannot register tickets");
        }
        SlaPolicy sla = slaPolicies.findByPriority(command.priority())
                .orElseThrow(() -> new IllegalStateException("No SLA policy configured for " + command.priority()));

        Instant now = clock.instant();
        Ticket ticket = tickets.save(Ticket.open(requester.id(), command.title().trim(),
                command.description().trim(), command.category(), command.priority(), command.site().trim(),
                command.affectedAsset().trim(), now, sla));
        history.append(new TicketHistoryEntry(null, ticket.id(), HistoryAction.CREATED, null, TicketStatus.ABIERTO,
                actor.email(), "Ticket registered with SLA deadline " + ticket.slaDueAt(), now));
        metrics.ticketCreated(ticket.category(), ticket.priority());
        log.info("Ticket {} created by user {} ({}/{})", ticket.id(), requester.id(), ticket.category(),
                ticket.priority());

        assignAutomatically(ticket);
        return tickets.findViewById(ticket.id()).orElseThrow();
    }

    private void assignAutomatically(Ticket ticket) {
        DispatchSuggestion suggestion = requestSuggestion(ticket);
        List<AssignmentSelection> ranking = assignmentPolicy.rank(ticket, suggestion,
                technicians.findAllWithWorkload(), clock.instant());

        for (AssignmentSelection selection : ranking) {
            // Re-check under a row lock: another transaction may have taken the last slot meanwhile.
            var locked = technicians.findByIdForUpdate(selection.technician().id());
            int active = technicians.countActiveTickets(selection.technician().id());
            if (locked.isPresent() && locked.get().isActive()
                    && locked.get().hasCapacityFor(ticket.priority(), active)) {
                assigner.assign(ticket, selection, TicketHistoryEntry.SYSTEM, HistoryAction.ASSIGNED,
                        discardedSuggestionNote(suggestion, selection));
                log.info("Ticket {} assigned to {} by {}", ticket.id(), selection.technician().code(),
                        selection.source());
                return;
            }
        }
        history.append(new TicketHistoryEntry(null, ticket.id(), HistoryAction.ASSIGNMENT_PENDING,
                TicketStatus.ABIERTO, TicketStatus.ABIERTO, TicketHistoryEntry.SYSTEM,
                "No eligible technician available; waiting for a manual assignment", clock.instant()));
        log.warn("Ticket {} stays ABIERTO: no eligible technician", ticket.id());
    }

    private DispatchSuggestion requestSuggestion(Ticket ticket) {
        try {
            return dispatch.suggest(ticket.id(), ticket.category(), ticket.priority());
        } catch (TechnicianDispatchUnavailableException ex) {
            log.warn("Dispatch service unavailable for ticket {}, using internal rules only: {}", ticket.id(),
                    ex.getMessage());
            return null;
        }
    }

    private String discardedSuggestionNote(DispatchSuggestion suggestion, AssignmentSelection selection) {
        if (suggestion == null) {
            return "Dispatch suggestion unavailable";
        }
        return selection.source() == AssignmentSource.DISPATCH ? null
                : "Dispatch suggestion " + suggestion.technicianCode() + " rejected by assignment rules";
    }
}
