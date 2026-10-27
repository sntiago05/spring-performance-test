package com.helpflow.dispatch.infrastructure.adapter.in.web;

import com.helpflow.dispatch.application.port.in.SuggestTechnicianUseCase;
import com.helpflow.dispatch.infrastructure.adapter.in.web.dto.DispatchRequest;
import com.helpflow.dispatch.infrastructure.adapter.in.web.dto.DispatchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Technician dispatch", description = "Simulated technician availability lookup")
class TechnicianDispatchController {

    private final SuggestTechnicianUseCase suggestTechnician;

    TechnicianDispatchController(SuggestTechnicianUseCase suggestTechnician) {
        this.suggestTechnician = suggestTechnician;
    }

    @Operation(
            summary = "Suggest a technician for a ticket",
            description = """
                    Returns the technician that would attend the ticket and the estimated time in hours.

                    **How it works:** `ticketId|categoria|prioridad` is folded into a numeric seed (CRC32); the seed \
                    selects the technician (`TEC-001` .. `TEC-<pool size>`) and an estimate between 0 and 12 hours.

                    **Availability buckets:** 0-2 h → `ALTA`, 3-8 h → `MEDIA`, more than 8 h → `BAJA`.

                    **Test cases**
                    1. Same body twice → identical responses (deterministic).
                    2. Change only `prioridad` → may return a different technician/estimate.
                    3. Missing or unknown `categoria`/`prioridad`, or `ticketId <= 0` → `400` ProblemDetail.
                    """)
    @ApiResponse(responseCode = "200", description = "Suggestion computed",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DispatchResponse.class),
                    examples = @ExampleObject(value = """
                            {"ticketId": 125, "tecnicoId": "TEC-014", "disponibilidad": "MEDIA", "tiempoEstimadoHoras": 4}""")))
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = """
                            {"type": "urn:helpflow:problem:validation-error", "title": "Validation failed", "status": 400,
                             "detail": "The request contains invalid fields.", "instance": "/technician-dispatch",
                             "timestamp": "2026-01-15T10:15:30Z", "traceId": "6f1c2b7e9a4d4c1f8e0b5a3d2c7e9f10",
                             "errors": [{"field": "ticketId", "message": "ticketId must be a positive number"}]}""")))
    @PostMapping(path = "/technician-dispatch",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    DispatchResponse dispatch(@Valid @RequestBody DispatchRequest request) {
        var suggestion = suggestTechnician.suggest(new SuggestTechnicianUseCase.Command(
                request.ticketId(), request.categoria(), request.prioridad()));
        return DispatchResponse.from(suggestion);
    }
}
