package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReassignRequest(
        @Schema(description = "Id (not the TEC- code) of the technician", example = "8")
        @NotNull(message = "{validation.required}")
        @Positive(message = "{validation.range}")
        Long technicianId,

        @Schema(example = "Reassigned to the network specialist of the same site")
        @NotBlank(message = "{validation.required}")
        @Size(max = 1000, message = "{validation.size}")
        String observations) {
}
