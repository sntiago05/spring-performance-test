package com.helpflow.dispatch.infrastructure.adapter.in.web;

import com.helpflow.dispatch.application.port.in.SuggestTechnicianUseCase;
import com.helpflow.dispatch.domain.model.TechnicianSuggestion;
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
@Tag(name = "Technician Suggestions", description = "Simulated technician availability lookup")
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
    @ApiResponse(responseCode = "200", description = "Suggestion computed successfully",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = DispatchResponse.class),
            examples = @ExampleObject(value = """
                {"ticketId": 125, "tecnicoId": "TEC-014", "disponibilidad": "MEDIA", "tiempoEstimadoHoras": 4}""")))
    @ApiResponse(responseCode = "400", description = "Invalid request body or validation failed",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = """
                {"type": "urn:helpflow:problem:validation-error", "title": "Validation failed", "status": 400,
                 "detail": "The request contains invalid fields.", "instance": "/technician-dispatch",
                 "timestamp": "2026-01-15T10:15:30Z", "traceId": "6f1c2b7e9a4d4c1f8e0b5a3d2c7e9f10",
                 "errors": [{"field": "ticketId", "message": "ticketId must be a positive number"}]}""")))
    @ApiResponse(responseCode = "404", description = "Resource or endpoint not found",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = """
                {"type": "urn:helpflow:problem:resource-not-found", "title": "Resource not found", "status": 404,
                 "detail": "The requested resource or endpoint was not found.", "instance": "/technician-dispatch/unknown",
                 "timestamp": "2026-01-15T10:15:30Z", "traceId": "6f1c2b7e9a4d4c1f8e0b5a3d2c7e9f10"}""")))
    @ApiResponse(responseCode = "405", description = "HTTP method not allowed for this endpoint",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = """
                {"type": "urn:helpflow:problem:method-not-allowed", "title": "Method not allowed", "status": 405,
                 "detail": "The HTTP method is not supported for this endpoint.", "instance": "/technician-dispatch",
                 "timestamp": "2026-01-15T10:15:30Z", "traceId": "6f1c2b7e9a4d4c1f8e0b5a3d2c7e9f10"}""")))
    @ApiResponse(responseCode = "406", description = "Requested representation not acceptable",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = """
                {"type": "urn:helpflow:problem:not-acceptable", "title": "Not acceptable", "status": 406,
                 "detail": "The requested representation cannot be served in the accepted format.", "instance": "/technician-dispatch",
                 "timestamp": "2026-01-15T10:15:30Z", "traceId": "6f1c2b7e9a4d4c1f8e0b5a3d2c7e9f10"}""")))
    @ApiResponse(responseCode = "415", description = "Unsupported payload media type",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = """
                {"type": "urn:helpflow:problem:unsupported-media-type", "title": "Unsupported media type", "status": 415,
                 "detail": "The requested media type is not supported.", "instance": "/technician-dispatch",
                 "timestamp": "2026-01-15T10:15:30Z", "traceId": "6f1c2b7e9a4d4c1f8e0b5a3d2c7e9f10"}""")))
    @ApiResponse(responseCode = "422", description = "Domain rule violation or unprocessable entity",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = """
                {"type": "urn:helpflow:problem:domain-rule-violation", "title": "Unprocessable entity", "status": 422,
                 "detail": "The request violates a domain rule.", "instance": "/technician-dispatch",
                 "timestamp": "2026-01-15T10:15:30Z", "traceId": "6f1c2b7e9a4d4c1f8e0b5a3d2c7e9f10"}""")))
    @ApiResponse(responseCode = "500", description = "Unexpected internal server error",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            schema = @Schema(implementation = ProblemDetail.class),
            examples = @ExampleObject(value = """
                {"type": "urn:helpflow:problem:internal-error", "title": "Internal error", "status": 500,
                 "detail": "An unexpected error occurred. Quote the traceId when reporting it.", "instance": "/technician-dispatch",
                 "timestamp": "2026-01-15T10:15:30Z", "traceId": "6f1c2b7e9a4d4c1f8e0b5a3d2c7e9f10"}""")))
    @PostMapping(path = "/technician-dispatch",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    DispatchResponse suggest(@Valid @RequestBody DispatchRequest request) {
        TechnicianSuggestion suggestion = suggestTechnician.suggest(new SuggestTechnicianUseCase.Command(
            request.ticketId(), request.categoria(), request.prioridad()));
        return DispatchResponse.from(suggestion);
    }
}
