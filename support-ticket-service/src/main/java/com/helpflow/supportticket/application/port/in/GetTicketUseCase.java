package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.TicketAssignment;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import java.util.List;

/**
 * Ticket consultation. Users see their own tickets, technicians the ones assigned to them and
 * admins every ticket.
 */
public interface GetTicketUseCase {

    TicketView get(Actor actor, Long ticketId);

    List<TicketHistoryEntry> history(Actor actor, Long ticketId);

    List<TicketAssignment> assignments(Actor actor, Long ticketId);
}
