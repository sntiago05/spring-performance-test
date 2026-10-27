package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record TicketResponse(
        @Schema(example = "125") Long id,
        @Schema(example = "Office Wi-Fi drops every few minutes") String title,
        String description,
        Category category,
        @Schema(description = "Current priority; it rises by one level when the ticket is auto-escalated")
        Priority priority,
        @Schema(example = "BOGOTA") String site,
        @Schema(example = "AP-3F-02") String affectedAsset,
        TicketStatus status,
        Instant createdAt,
        @Schema(description = "SLA deadline calculated at creation from the priority") Instant slaDueAt,
        @Schema(description = "True once the SLA deadline was exceeded while the ticket was unresolved")
        boolean slaBreached,
        @Schema(description = "True once the ticket was auto-escalated for being close to its deadline")
        boolean escalated,
        Instant escalatedAt,
        @Schema(description = "When a technician first moved the ticket to EN_PROGRESO") Instant firstResponseAt,
        Instant resolvedAt,
        UserRef requester,
        @Schema(description = "Null while the ticket is ABIERTO without assignment") TechnicianRef technician) {

    public record UserRef(Long id, String fullName, String email) {
    }

    public record TechnicianRef(Long id, String code, String fullName, Category specialty) {
    }
}
