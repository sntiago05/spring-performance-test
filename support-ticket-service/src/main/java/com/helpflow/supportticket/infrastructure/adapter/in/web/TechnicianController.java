package com.helpflow.supportticket.infrastructure.adapter.in.web;

import com.helpflow.supportticket.application.port.in.ManageTechnicianUseCase;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.UserStatus;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.CreateTechnicianRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.PageResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.TechnicianResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.UpdateTechnicianRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/technicians")
@Tag(name = "Technicians", description = "Support team profiles (ADMIN only). Fifteen technicians TEC-001..TEC-015 are seeded")
@SecurityRequirement(name = "bearerAuth")
class TechnicianController {

    private static final Set<String> SORT_FIELDS = Set.of("id", "code", "fullName", "site", "specialty");

    private final ManageTechnicianUseCase manageTechnicians;
    private final WebMapper mapper;

    TechnicianController(ManageTechnicianUseCase manageTechnicians, WebMapper mapper) {
        this.manageTechnicians = manageTechnicians;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Create a technician (ADMIN)",
            description = """
                    Creates, in one transaction, the login account (role **ROLE_TECNICO**) and the support profile. \
                    The code (`TEC-016`, `TEC-017`, ...) comes from a database sequence.

                    `specialty` selects the tickets the technician can take: the same category, or any category \
                    when the specialty is `OTRO` (generalist). `maxActiveTickets` caps simultaneous tickets in \
                    ASIGNADO/EN_PROGRESO (CRITICA tickets may use one extra slot).

                    **Test cases:** valid → `201` with `code`; duplicated e-mail/document → `409`; public e-mail → `400`; \
                    non-admin token → `403`.
                    """)
    @ApiResponse(responseCode = "201", description = "Technician created")
    @ApiResponse(responseCode = "409", description = "Document or e-mail already registered")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TechnicianResponse create(@Valid @RequestBody CreateTechnicianRequest request) {
        return mapper.toResponse(manageTechnicians.create(new ManageTechnicianUseCase.CreateCommand(
                request.document(), request.fullName(), request.email(), request.password(), request.site(),
                request.area(), request.specialty(), request.maxActiveTickets())));
    }

    @Operation(summary = "List technicians (ADMIN)",
            description = "Paginated, filterable by `status` and `specialty`; default order by `code`.")
    @GetMapping
    PageResponse<TechnicianResponse> list(
            @RequestParam(required = false) UserStatus status,
            @Parameter(description = "HARDWARE, SOFTWARE, RED, ACCESOS or OTRO") @RequestParam(required = false) Category specialty,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(example = "code,asc") @RequestParam(required = false) String sort) {
        var result = manageTechnicians.list(status, specialty, PageParams.of(page, size, sort, SORT_FIELDS, "code", false));
        return mapper.toPage(result, mapper::toResponse);
    }

    @Operation(summary = "Get a technician (ADMIN)")
    @ApiResponse(responseCode = "404", description = "Technician not found")
    @GetMapping("/{id}")
    TechnicianResponse get(@PathVariable Long id) {
        return mapper.toResponse(manageTechnicians.get(id));
    }

    @Operation(
            summary = "Edit a technician profile (ADMIN)",
            description = """
                    Updates name, specialty, site, capacity and status. Setting `INACTIVO` removes the technician \
                    from automatic assignment and rejects manual assignments to them (`422` `technician-inactive`); \
                    tickets they already hold stay assigned until an admin reassigns them.
                    """)
    @ApiResponse(responseCode = "404", description = "Technician not found")
    @PutMapping("/{id}")
    TechnicianResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTechnicianRequest request) {
        return mapper.toResponse(manageTechnicians.update(id, new ManageTechnicianUseCase.UpdateCommand(
                request.fullName(), request.specialty(), request.site(), request.maxActiveTickets(),
                request.status())));
    }
}
