package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.port.in.GetTicketUseCase;
import com.helpflow.supportticket.application.port.out.TicketAssignmentPort;
import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.exception.ResourceNotFoundException;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.TicketAssignment;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetTicketService implements GetTicketUseCase {

    private final TicketRepositoryPort tickets;
    private final TicketHistoryPort history;
    private final TicketAssignmentPort assignments;
    private final TicketAccess access;

    GetTicketService(TicketRepositoryPort tickets, TicketHistoryPort history, TicketAssignmentPort assignments,
            TicketAccess access) {
        this.tickets = tickets;
        this.history = history;
        this.assignments = assignments;
        this.access = access;
    }

    @Override
    public TicketView get(Actor actor, Long ticketId) {
        TicketView view = tickets.findViewById(ticketId).orElseThrow(() -> notFound(ticketId));
        access.assertCanView(actor, view.ticket());
        return view;
    }

    @Override
    public List<TicketHistoryEntry> history(Actor actor, Long ticketId) {
        get(actor, ticketId);
        return history.findByTicketId(ticketId);
    }

    @Override
    public List<TicketAssignment> assignments(Actor actor, Long ticketId) {
        get(actor, ticketId);
        return assignments.findByTicketId(ticketId);
    }

    static ResourceNotFoundException notFound(Long ticketId) {
        return new ResourceNotFoundException("ticket-not-found", "Ticket " + ticketId + " not found", ticketId);
    }
}
