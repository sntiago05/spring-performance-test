package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
        @Schema(example = "7") Long id,
        @Schema(example = "1020304050") String document,
        @Schema(example = "Laura Gomez") String fullName,
        @Schema(example = "laura.gomez@helpflow.example") String email,
        @Schema(example = "BOGOTA") String site,
        @Schema(example = "Finance") String area,
        UserStatus status,
        Role role) {
}
