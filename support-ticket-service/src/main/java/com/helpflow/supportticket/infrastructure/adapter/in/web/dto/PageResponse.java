package com.helpflow.supportticket.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PageResponse<T>(
        List<T> content,
        @Schema(description = "Zero-based page index", example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(example = "57") long totalElements,
        @Schema(example = "3") int totalPages) {
}
