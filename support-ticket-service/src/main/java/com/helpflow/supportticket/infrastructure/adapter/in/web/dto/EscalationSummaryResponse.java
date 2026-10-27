package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EscalationSummaryResponse(
        @Schema(description = "Tickets inspected") int reviewed,
        @Schema(description = "Tickets escalated in this run") int escalated,
        @Schema(description = "Tickets newly flagged as SLA breached in this run") int breached,
        @Schema(description = "Tickets whose review failed (see logs)") int failed) {
}
