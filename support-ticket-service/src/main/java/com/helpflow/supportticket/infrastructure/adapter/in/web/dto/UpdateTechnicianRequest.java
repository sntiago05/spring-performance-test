package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTechnicianRequest(
        @Schema(example = "Carlos Ruiz Mora")
        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 120, message = "{validation.size}")
        String fullName,

        @NotNull(message = "{validation.required}")
        Category specialty,

        @Schema(example = "CALI")
        @NotBlank(message = "{validation.required}")
        @Size(max = 60, message = "{validation.size}")
        String site,

        @Schema(example = "5")
        @Min(value = 1, message = "{validation.range}")
        @Max(value = 20, message = "{validation.range}")
        int maxActiveTickets,

        @Schema(description = "INACTIVO technicians receive no new assignments", example = "ACTIVO")
        @NotNull(message = "{validation.required}")
        UserStatus status) {
}
