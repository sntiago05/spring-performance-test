package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Editable basic information. Document and e-mail cannot be changed.")
public record UpdateProfileRequest(
        @Schema(example = "Laura Gomez Perez")
        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 120, message = "{validation.size}")
        String fullName,

        @Schema(example = "MEDELLIN")
        @NotBlank(message = "{validation.required}")
        @Size(max = 60, message = "{validation.size}")
        String site,

        @Schema(example = "Accounting")
        @NotBlank(message = "{validation.required}")
        @Size(max = 60, message = "{validation.size}")
        String area) {
}
