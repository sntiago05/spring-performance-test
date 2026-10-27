package com.helpflow.supportticket.domain.model;

import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.InvalidTicketTransitionException;
import java.time.Duration;
import java.time.Instant;

/**
 * Ticket aggregate. Immutable: every behavior returns the updated copy, so state changes are
 * explicit and the persistence adapter decides when to store them.
 */
public record Ticket(
        Long id,
        Long requesterId,
        String title,
        String description,
        Category category,
        Priority priority,
        String site,
        String affectedAsset,
        TicketStatus status,
        Instant createdAt,
        Instant slaDueAt,
        Long assignedTechnicianId,
        Instant firstResponseAt,
        Instant resolvedAt,
        boolean escalated,
        Instant escalatedAt,
        boolean slaBreached,
        Long version) {

    public Ticket {
        if (slaDueAt != null && createdAt != null && !slaDueAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("The SLA deadline must be after the creation date");
        }
    }

    /**
     * Registers a new ticket in {@code ABIERTO} state with the SLA deadline of its priority.
     */
    public static Ticket open(Long requesterId, String title, String description, Category category,
            Priority priority, String site, String affectedAsset, Instant now, SlaPolicy slaPolicy) {
        return new Ticket(null, requesterId, title, description, category, priority, site, affectedAsset,
                TicketStatus.ABIERTO, now, slaPolicy.dueAt(now), null, null, null, false, null, false, null);
    }

    public boolean isOwnedBy(Long userId) {
        return requesterId.equals(userId);
    }

    public boolean isAssignedTo(Long technicianId) {
        return assignedTechnicianId != null && assignedTechnicianId.equals(technicianId);
    }

    /**
     * Gives the ticket to a technician. New tickets move to {@code ASIGNADO}; tickets already being
     * worked on go back to {@code ASIGNADO} so the new owner has to take them again.
     */
    public Ticket assignTo(Long technicianId) {
        if (!status.isSlaRunning()) {
            throw new BusinessRuleViolationException("ticket-not-assignable",
                    "Ticket in status " + status + " cannot be assigned", status);
        }
        return copy(TicketStatus.ASIGNADO, technicianId, firstResponseAt, resolvedAt, priority, escalated,
                escalatedAt, slaBreached);
    }

    /**
     * Moves the ticket along the state machine. {@code ASIGNADO} is reachable only through
     * {@link #assignTo(Long)} because it needs a technician.
     */
    public Ticket transitionTo(TicketStatus target, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidTicketTransitionException(status, target);
        }
        if (target == TicketStatus.ASIGNADO) {
            throw new BusinessRuleViolationException("assignment-required",
                    "Status ASIGNADO can only be reached by assigning a technician");
        }
        Instant newFirstResponse = firstResponseAt;
        Instant newResolvedAt = resolvedAt;
        boolean newBreached = slaBreached;
        if (target == TicketStatus.EN_PROGRESO) {
            newFirstResponse = firstResponseAt == null ? now : firstResponseAt;
            newResolvedAt = null;
        }
        if (target == TicketStatus.RESUELTO) {
            newResolvedAt = now;
            newBreached = slaBreached || now.isAfter(slaDueAt);
        }
        return copy(target, assignedTechnicianId, newFirstResponse, newResolvedAt, priority, escalated,
                escalatedAt, newBreached);
    }

    public Ticket escalate(Instant now) {
        return copy(status, assignedTechnicianId, firstResponseAt, resolvedAt, priority.escalate(), true, now,
                slaBreached);
    }

    public Ticket markSlaBreached() {
        return copy(status, assignedTechnicianId, firstResponseAt, resolvedAt, priority, escalated, escalatedAt,
                true);
    }

    public boolean isOverdue(Instant now) {
        return status.isSlaRunning() && now.isAfter(slaDueAt);
    }

    /**
     * Share of the SLA window already consumed, as a percentage (may exceed 100).
     */
    public double elapsedSlaPercent(Instant now) {
        double total = Duration.between(createdAt, slaDueAt).toMillis();
        double elapsed = Duration.between(createdAt, now).toMillis();
        return elapsed / total * 100;
    }

    public boolean needsEscalation(Instant now, int thresholdPercent) {
        return status.isSlaRunning() && !escalated && elapsedSlaPercent(now) >= thresholdPercent;
    }

    private Ticket copy(TicketStatus newStatus, Long technicianId, Instant newFirstResponse, Instant newResolvedAt,
            Priority newPriority, boolean newEscalated, Instant newEscalatedAt, boolean newBreached) {
        return new Ticket(id, requesterId, title, description, category, newPriority, site, affectedAsset,
                newStatus, createdAt, slaDueAt, technicianId, newFirstResponse, newResolvedAt, newEscalated,
                newEscalatedAt, newBreached, version);
    }
}
