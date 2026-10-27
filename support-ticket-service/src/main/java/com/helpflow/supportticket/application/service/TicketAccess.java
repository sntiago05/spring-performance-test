package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.domain.exception.ForbiddenOperationException;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.Ticket;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Ownership rules: users reach their own tickets, technicians the tickets assigned to them, admins
 * everything. Roles are checked at the HTTP edge; this is the data-level authorization.
 */
@Component
class TicketAccess {

    private final TechnicianRepositoryPort technicians;

    TicketAccess(TechnicianRepositoryPort technicians) {
        this.technicians = technicians;
    }

    void assertCanView(Actor actor, Ticket ticket) {
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isRegularUser() && ticket.isOwnedBy(actor.userId())) {
            return;
        }
        if (actor.isTechnician() && isAssignedTechnician(actor, ticket)) {
            return;
        }
        throw new ForbiddenOperationException("ticket-access-denied",
                "User " + actor.userId() + " cannot access ticket " + ticket.id());
    }

    void assertCanManage(Actor actor, Ticket ticket) {
        if (actor.isAdmin()) {
            return;
        }
        if (actor.isTechnician() && isAssignedTechnician(actor, ticket)) {
            return;
        }
        throw new ForbiddenOperationException("ticket-manage-denied",
                "User " + actor.userId() + " cannot manage ticket " + ticket.id());
    }

    Optional<Technician> technicianOf(Actor actor) {
        return technicians.findByUserId(actor.userId());
    }

    private boolean isAssignedTechnician(Actor actor, Ticket ticket) {
        return technicianOf(actor).filter(technician -> ticket.isAssignedTo(technician.id())).isPresent();
    }
}
