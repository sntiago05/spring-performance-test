package com.helpflow.supportticket.domain.exception;

import com.helpflow.supportticket.domain.model.TicketStatus;

public class InvalidTicketTransitionException extends DomainException {

    public InvalidTicketTransitionException(TicketStatus from, TicketStatus to) {
        super("invalid-transition", "Transition " + from + " -> " + to + " is not allowed", from, to);
    }
}
