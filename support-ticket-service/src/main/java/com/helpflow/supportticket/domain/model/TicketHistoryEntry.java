package com.helpflow.supportticket.domain.model;

import java.time.Instant;

/**
 * Immutable audit record of something that happened to a ticket.
 */
public record TicketHistoryEntry(
        Long id,
        Long ticketId,
        HistoryAction action,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        String performedBy,
        String observations,
        Instant occurredAt) {

    public static final String SYSTEM = "SYSTEM";
}
