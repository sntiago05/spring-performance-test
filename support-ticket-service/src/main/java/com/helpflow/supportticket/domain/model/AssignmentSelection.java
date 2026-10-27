package com.helpflow.supportticket.domain.model;

/**
 * A technician chosen (or ranked) for a ticket together with how and why.
 *
 * @param withinSla {@code true}/{@code false} when a dispatch estimate was available to compare with
 *                  the SLA deadline, {@code null} when the estimate is unknown (rule-based choice)
 */
public record AssignmentSelection(
        Technician technician,
        AssignmentSource source,
        Integer estimatedHours,
        Availability availability,
        Boolean withinSla) {
}
