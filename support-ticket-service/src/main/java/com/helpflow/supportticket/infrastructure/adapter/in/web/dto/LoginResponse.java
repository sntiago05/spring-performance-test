package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record LoginResponse(
        @Schema(description = "JWT to send as `Authorization: Bearer <token>`") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Instant at which the token stops being valid") Instant expiresAt,
        UserResponse user) {
}
