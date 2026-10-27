package com.helpflow.dispatch.infrastructure.adapter.in.web.dto;

import com.helpflow.dispatch.domain.model.Category;
import com.helpflow.dispatch.domain.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Ticket data used to simulate the technician lookup")
public record DispatchRequest(
        @Schema(description = "Ticket identifier (any positive number)", example = "125")
        @NotNull(message = "{validation.required}")
        @Positive(message = "{validation.ticketId.positive}")
        Long ticketId,

        @Schema(description = "Ticket category", example = "RED")
        @NotNull(message = "{validation.required}")
        Category categoria,

        @Schema(description = "Ticket priority. `CRITICA` is also accepted as `CRÍTICA`", example = "ALTA")
        @NotNull(message = "{validation.required}")
        Priority prioridad) {
}
