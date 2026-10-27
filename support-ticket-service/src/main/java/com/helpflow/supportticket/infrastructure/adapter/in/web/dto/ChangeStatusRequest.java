package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.infrastructure.adapter.in.web.validation.ValidStatusChange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidStatusChange
public record ChangeStatusRequest(
        @Schema(description = "Target status. ASIGNADO is not accepted here: use the assignment endpoint",
                example = "EN_PROGRESO")
        @NotNull(message = "{validation.required}")
        TicketStatus status,

        @Schema(description = "Mandatory observations stored in the ticket history (min. 15 characters to cancel)",
                example = "Technician on site, checking the access point")
        @NotBlank(message = "{validation.required}")
        @Size(max = 1000, message = "{validation.size}")
        String observations) {
}
