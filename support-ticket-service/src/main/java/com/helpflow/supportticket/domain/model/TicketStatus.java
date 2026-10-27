package com.helpflow.supportticket.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ticket lifecycle. The allowed transitions are the single source of truth for the state machine:
 *
 * <pre>
 * ABIERTO ──► ASIGNADO ──► EN_PROGRESO ──► RESUELTO ──► CERRADO
 *    │            │  ▲          │  ▲           │
 *    │            │  └──────────┘  └───────────┘ (reassignment / reopening)
 *    └────────────┴──────────────┴──► CANCELADO
 * </pre>
 */
public enum TicketStatus {
    ABIERTO, ASIGNADO, EN_PROGRESO, RESUELTO, CERRADO, CANCELADO;

    public Set<TicketStatus> allowedTargets() {
        return switch (this) {
            case ABIERTO -> EnumSet.of(ASIGNADO, CANCELADO);
            case ASIGNADO -> EnumSet.of(EN_PROGRESO, CANCELADO);
            case EN_PROGRESO -> EnumSet.of(ASIGNADO, RESUELTO, CANCELADO);
            case RESUELTO -> EnumSet.of(EN_PROGRESO, CERRADO);
            case CERRADO, CANCELADO -> EnumSet.noneOf(TicketStatus.class);
        };
    }

    public boolean canTransitionTo(TicketStatus target) {
        return allowedTargets().contains(target);
    }

    /**
     * Tickets that still consume SLA time (no one has delivered a resolution yet).
     */
    public boolean isSlaRunning() {
        return this == ABIERTO || this == ASIGNADO || this == EN_PROGRESO;
    }

    /**
     * Tickets that count against a technician's workload.
     */
    public boolean isTechnicianWorkload() {
        return this == ASIGNADO || this == EN_PROGRESO;
    }
}
