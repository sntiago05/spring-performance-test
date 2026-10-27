package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.TicketSearchCriteria;
import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.port.in.SearchTicketsUseCase;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.exception.ForbiddenOperationException;
import com.helpflow.supportticket.domain.model.Actor;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class SearchTicketsService implements SearchTicketsUseCase {

    private final TicketRepositoryPort tickets;
    private final TicketAccess access;
    private final Clock clock;

    SearchTicketsService(TicketRepositoryPort tickets, TicketAccess access, Clock clock) {
        this.tickets = tickets;
        this.access = access;
        this.clock = clock;
    }

    @Override
    public PageResult<TicketView> search(Actor actor, Filter filter, PageQuery page) {
        TicketSearchCriteria criteria = new TicketSearchCriteria(filter.requesterId(), filter.technicianId(),
                filter.status(), filter.priority(), filter.category(),
                filter.overdueOnly() ? clock.instant() : null);

        if (actor.isRegularUser()) {
            criteria = criteria.withRequesterId(actor.userId()).withTechnicianId(null);
        } else if (actor.isTechnician()) {
            var technician = access.technicianOf(actor).orElseThrow(() -> new ForbiddenOperationException(
                    "technician-profile-missing", "User " + actor.userId() + " has no technician profile"));
            criteria = criteria.withTechnicianId(technician.id()).withRequesterId(null);
        }
        return tickets.search(criteria, page);
    }
}
