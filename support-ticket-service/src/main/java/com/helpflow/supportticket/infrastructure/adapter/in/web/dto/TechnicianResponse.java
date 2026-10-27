package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record TechnicianResponse(
        @Schema(example = "8") Long id,
        @Schema(example = "TEC-008") String code,
        @Schema(description = "Login account, null for seeded technicians without one") Long userId,
        @Schema(example = "Network Technician Medellin") String fullName,
        Category specialty,
        @Schema(example = "MEDELLIN") String site,
        @Schema(example = "3") int maxActiveTickets,
        UserStatus status) {
}
