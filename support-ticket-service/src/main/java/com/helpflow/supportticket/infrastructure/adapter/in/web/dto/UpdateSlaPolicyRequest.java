package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateSlaPolicyRequest(
        @Schema(example = "6")
        @Min(value = 1, message = "{validation.range}")
        @Max(value = 720, message = "{validation.range}")
        int resolutionHours,

        @Schema(example = "75")
        @Min(value = 1, message = "{validation.range}")
        @Max(value = 99, message = "{validation.range}")
        int escalationThresholdPercent) {
}
