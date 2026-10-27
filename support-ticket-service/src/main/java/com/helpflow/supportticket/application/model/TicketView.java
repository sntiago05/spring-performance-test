package com.helpflow.supportticket.application.model;

import com.helpflow.supportticket.domain.model.Ticket;

/**
 * Read model: a ticket together with the people involved, loaded in a single query.
 */
public record TicketView(Ticket ticket, UserSummary requester, TechnicianSummary technician) {
}
