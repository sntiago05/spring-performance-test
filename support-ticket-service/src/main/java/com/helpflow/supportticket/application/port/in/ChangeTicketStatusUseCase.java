package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.TicketStatus;

public interface ChangeTicketStatusUseCase {

    /**
     * Applies a valid state transition and records it, with the observations, in the ticket history.
     * Technicians can only act on tickets assigned to them and cannot cancel.
     */
    TicketView changeStatus(Actor actor, Long ticketId, TicketStatus target, String observations);
}
