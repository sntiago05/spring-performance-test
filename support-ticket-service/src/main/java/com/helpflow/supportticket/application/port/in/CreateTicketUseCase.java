package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;

public interface CreateTicketUseCase {

    /**
     * Registers the ticket, calculates its SLA deadline and tries the automatic assignment. When no
     * technician can take it the ticket stays ABIERTO.
     */
    TicketView create(Actor actor, Command command);

    record Command(String title, String description, Category category, Priority priority, String site,
            String affectedAsset) {
    }
}
