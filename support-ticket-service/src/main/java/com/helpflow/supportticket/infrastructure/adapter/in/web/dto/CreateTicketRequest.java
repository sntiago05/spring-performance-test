package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @Schema(description = "Short summary", example = "Office Wi-Fi drops every few minutes")
        @NotBlank(message = "{validation.required}")
        @Size(min = 5, max = 150, message = "{validation.size}")
        String title,

        @Schema(description = "Detailed description of the problem",
                example = "Since this morning the Wi-Fi in the 3rd floor disconnects every 5 minutes.")
        @NotBlank(message = "{validation.required}")
        @Size(min = 10, max = 4000, message = "{validation.size}")
        String description,

        @Schema(description = "HARDWARE, SOFTWARE, RED, ACCESOS or OTRO", example = "RED")
        @NotNull(message = "{validation.required}")
        Category category,

        @Schema(description = "BAJA, MEDIA, ALTA or CRITICA (the accented CRÍTICA is accepted too). Determines the SLA window",
                example = "ALTA")
        @NotNull(message = "{validation.required}")
        Priority priority,

        @Schema(description = "Site where the problem happens", example = "BOGOTA")
        @NotBlank(message = "{validation.required}")
        @Size(max = 60, message = "{validation.size}")
        String site,

        @Schema(description = "Affected asset (hostname, inventory tag, application...)", example = "AP-3F-02")
        @NotBlank(message = "{validation.required}")
        @Size(max = 120, message = "{validation.size}")
        String affectedAsset) {
}
