package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.AssignmentSource;
import com.helpflow.supportticket.domain.model.Availability;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record AssignmentResponse(
        Long id,
        Long technicianId,
        @Schema(description = "DISPATCH = suggested by technician-dispatch-service, RULES = internal rules "
                + "(dispatch unavailable or rejected), MANUAL = admin decision") AssignmentSource source,
        @Schema(description = "Estimate returned by dispatch, null when unknown") Integer estimatedHours,
        Availability availability,
        @Schema(description = "Whether the dispatch estimate fits the SLA window; null when no estimate")
        Boolean withinSla,
        String assignedBy,
        Instant assignedAt,
        @Schema(description = "True for the current assignment, false for previous owners") boolean active) {
}
