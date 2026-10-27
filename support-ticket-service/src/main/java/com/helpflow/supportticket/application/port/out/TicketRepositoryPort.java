package com.helpflow.supportticket.application.port.out;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.TicketSearchCriteria;
import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.domain.model.Ticket;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepositoryPort {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(Long id);

    Optional<TicketView> findViewById(Long id);

    PageResult<TicketView> search(TicketSearchCriteria criteria, PageQuery page);

    /**
     * Ids of tickets whose SLA is still running and that were not yet both escalated and flagged as
     * breached: the working set of the periodic SLA review.
     */
    List<Long> findIdsPendingSlaReview();

    long countOverdue(Instant now);
}
