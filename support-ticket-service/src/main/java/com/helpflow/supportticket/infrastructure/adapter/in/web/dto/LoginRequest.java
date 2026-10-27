package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "laura.gomez@helpflow.example")
        @NotBlank(message = "{validation.required}")
        String email,

        @Schema(example = "Str0ngPass!2026", format = "password")
        @NotBlank(message = "{validation.required}")
        String password) {
}
