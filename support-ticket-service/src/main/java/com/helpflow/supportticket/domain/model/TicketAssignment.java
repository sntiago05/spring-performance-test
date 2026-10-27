package com.helpflow.supportticket.domain.model;

import java.time.Instant;

/**
 * Link between a ticket and the technician in charge of it. At most one assignment per ticket is
 * active; earlier ones are kept as a record of previous owners.
 */
public record TicketAssignment(
        Long id,
        Long ticketId,
        Long technicianId,
        AssignmentSource source,
        Integer estimatedHours,
        Availability availability,
        Boolean withinSla,
        String assignedBy,
        Instant assignedAt,
        boolean active) {
}
