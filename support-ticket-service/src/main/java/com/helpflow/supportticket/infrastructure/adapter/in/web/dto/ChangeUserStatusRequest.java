package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(
        @Schema(description = "INACTIVO users cannot log in or register tickets", example = "INACTIVO")
        @NotNull(message = "{validation.required}")
        UserStatus status) {
}
