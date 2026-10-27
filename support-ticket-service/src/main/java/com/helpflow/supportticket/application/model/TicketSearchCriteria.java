package com.helpflow.supportticket.application.model;

import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.TicketStatus;
import java.time.Instant;

/**
 * @param overdueAt when set, only tickets whose SLA is still running and whose deadline is before
 *                  this instant are returned
 */
public record TicketSearchCriteria(
        Long requesterId,
        Long technicianId,
        TicketStatus status,
        Priority priority,
        Category category,
        Instant overdueAt) {

    public TicketSearchCriteria withRequesterId(Long id) {
        return new TicketSearchCriteria(id, technicianId, status, priority, category, overdueAt);
    }

    public TicketSearchCriteria withTechnicianId(Long id) {
        return new TicketSearchCriteria(requesterId, id, status, priority, category, overdueAt);
    }
}
