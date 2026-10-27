package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.port.in.ReassignTicketUseCase;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.ForbiddenOperationException;
import com.helpflow.supportticket.domain.exception.ResourceNotFoundException;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.AssignmentSelection;
import com.helpflow.supportticket.domain.model.AssignmentSource;
import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.Ticket;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ReassignTicketService implements ReassignTicketUseCase {

    private final TicketRepositoryPort tickets;
    private final TechnicianRepositoryPort technicians;
    private final TicketAssigner assigner;

    ReassignTicketService(TicketRepositoryPort tickets, TechnicianRepositoryPort technicians,
            TicketAssigner assigner) {
        this.tickets = tickets;
        this.technicians = technicians;
        this.assigner = assigner;
    }

    @Override
    public TicketView reassign(Actor actor, Long ticketId, Long technicianId, String observations) {
        if (!actor.isAdmin()) {
            throw new ForbiddenOperationException("reassign-admin-only", "Only admins can assign tickets");
        }
        Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> GetTicketService.notFound(ticketId));
        Technician technician = technicians.findByIdForUpdate(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("technician-not-found",
                        "Technician " + technicianId + " not found", technicianId));

        if (ticket.isAssignedTo(technicianId)) {
            throw new BusinessRuleViolationException("already-assigned",
                    "Ticket " + ticketId + " is already assigned to " + technician.code(), technician.code());
        }
        if (!technician.isActive()) {
            throw new BusinessRuleViolationException("technician-inactive",
                    "Technician " + technician.code() + " is not enabled", technician.code());
        }
        if (!technician.canHandle(ticket.category())) {
            throw new BusinessRuleViolationException("technician-incompatible",
                    "Technician " + technician.code() + " does not handle " + ticket.category(),
                    technician.code(), ticket.category());
        }
        if (!technician.hasCapacityFor(ticket.priority(), technicians.countActiveTickets(technicianId))) {
            throw new BusinessRuleViolationException("technician-at-capacity",
                    "Technician " + technician.code() + " reached " + technician.maxActiveTickets()
                            + " active tickets", technician.code(), technician.maxActiveTickets());
        }

        HistoryAction action = ticket.assignedTechnicianId() == null ? HistoryAction.ASSIGNED
                : HistoryAction.REASSIGNED;
        assigner.assign(ticket, new AssignmentSelection(technician, AssignmentSource.MANUAL, null, null, null),
                actor.email(), action, observations.trim());
        return tickets.findViewById(ticketId).orElseThrow();
    }
}
