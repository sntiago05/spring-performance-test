package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.domain.model.Actor;

public interface ReassignTicketUseCase {

    /**
     * Manually assigns (or reassigns) a ticket to an enabled, compatible technician with capacity.
     */
    TicketView reassign(Actor actor, Long ticketId, Long technicianId, String observations);
}
