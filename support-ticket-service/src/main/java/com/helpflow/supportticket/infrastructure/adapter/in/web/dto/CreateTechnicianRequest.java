package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.infrastructure.adapter.in.web.validation.CorporateEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Creates the technician's login account (ROLE_TECNICO) and support profile together.")
public record CreateTechnicianRequest(
        @Schema(example = "9988776655")
        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^[A-Za-z0-9-]{5,30}$", message = "{validation.document.format}")
        String document,

        @Schema(example = "Carlos Ruiz")
        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 120, message = "{validation.size}")
        String fullName,

        @Schema(example = "carlos.ruiz@helpflow.example")
        @NotBlank(message = "{validation.required}")
        @CorporateEmail
        String email,

        @Schema(format = "password", example = "Tech!Pass2026")
        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 72, message = "{validation.size}")
        String password,

        @Schema(example = "BOGOTA")
        @NotBlank(message = "{validation.required}")
        @Size(max = 60, message = "{validation.size}")
        String site,

        @Schema(example = "IT Support")
        @NotBlank(message = "{validation.required}")
        @Size(max = 60, message = "{validation.size}")
        String area,

        @Schema(description = "Category the technician attends. OTRO means generalist (any category)",
                example = "RED")
        @NotNull(message = "{validation.required}")
        Category specialty,

        @Schema(description = "Maximum simultaneous tickets in ASIGNADO or EN_PROGRESO", example = "4")
        @Min(value = 1, message = "{validation.range}")
        @Max(value = 20, message = "{validation.range}")
        int maxActiveTickets) {
}
