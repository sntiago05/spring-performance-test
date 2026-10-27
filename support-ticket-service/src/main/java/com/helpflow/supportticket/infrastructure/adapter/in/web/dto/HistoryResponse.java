package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record HistoryResponse(
        Long id,
        HistoryAction action,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        @Schema(description = "E-mail of the user who acted, or SYSTEM for automatic actions", example = "SYSTEM")
        String performedBy,
        String observations,
        Instant occurredAt) {
}
