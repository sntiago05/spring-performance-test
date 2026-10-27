package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.TicketStatus;

public interface SearchTicketsUseCase {

    /**
     * The result is always limited to what the actor may see: own tickets for users, assigned tickets
     * for technicians, everything (optionally filtered by requester/technician) for admins.
     */
    PageResult<TicketView> search(Actor actor, Filter filter, PageQuery page);

    record Filter(Long requesterId, Long technicianId, TicketStatus status, Priority priority, Category category,
            boolean overdueOnly) {
    }
}
