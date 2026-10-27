package com.helpflow.dispatch.infrastructure.adapter.in.web.dto;

import com.helpflow.dispatch.domain.model.Availability;
import com.helpflow.dispatch.domain.model.TechnicianSuggestion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Suggested technician and estimated attention time")
public record DispatchResponse(
        @Schema(example = "125") long ticketId,
        @Schema(description = "Suggested technician code", example = "TEC-014") String tecnicoId,
        @Schema(description = "ALTA for 0-2 hours, MEDIA for 3-8 hours, BAJA for more than 8 hours", example = "MEDIA")
        Availability disponibilidad,
        @Schema(description = "Estimated time until the technician can attend the ticket", example = "4")
        int tiempoEstimadoHoras) {

    public static DispatchResponse from(TechnicianSuggestion suggestion) {
        return new DispatchResponse(
                suggestion.ticketId(),
                suggestion.technicianId(),
                suggestion.availability(),
                suggestion.estimatedHours());
    }
}
