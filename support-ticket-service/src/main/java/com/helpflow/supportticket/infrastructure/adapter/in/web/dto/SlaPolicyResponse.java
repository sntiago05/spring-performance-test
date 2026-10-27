package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;

public record SlaPolicyResponse(
        Priority priority,
        @Schema(description = "Hours until the SLA deadline", example = "8") int resolutionHours,
        @Schema(description = "Share of the window after which the ticket is auto-escalated", example = "80")
        int escalationThresholdPercent) {
}
