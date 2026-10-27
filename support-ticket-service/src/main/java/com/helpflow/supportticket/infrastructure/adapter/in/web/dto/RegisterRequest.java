package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.infrastructure.adapter.in.web.validation.CorporateEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Self-registration data. The account is created ACTIVO with role ROLE_USUARIO.")
public record RegisterRequest(
        @Schema(description = "National document, unique", example = "1020304050")
        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^[A-Za-z0-9-]{5,30}$", message = "{validation.document.format}")
        String document,

        @Schema(description = "Full name", example = "Laura Gomez")
        @NotBlank(message = "{validation.required}")
        @Size(min = 2, max = 120, message = "{validation.size}")
        String fullName,

        @Schema(description = "Corporate e-mail, unique. Public providers (gmail, hotmail, ...) are rejected",
                example = "laura.gomez@helpflow.example")
        @NotBlank(message = "{validation.required}")
        @CorporateEmail
        String email,

        @Schema(description = "Password, 8 to 72 characters", example = "Str0ngPass!2026", format = "password")
        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 72, message = "{validation.size}")
        String password,

        @Schema(description = "Site (office) of the user", example = "BOGOTA")
        @NotBlank(message = "{validation.required}")
        @Size(max = 60, message = "{validation.size}")
        String site,

        @Schema(description = "Area or department", example = "Finance")
        @NotBlank(message = "{validation.required}")
        @Size(max = 60, message = "{validation.size}")
        String area) {
}
