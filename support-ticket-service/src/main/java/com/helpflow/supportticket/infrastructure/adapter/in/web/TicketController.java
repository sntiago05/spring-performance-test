package com.helpflow.supportticket.infrastructure.adapter.in.web;

import com.helpflow.supportticket.application.port.in.ChangeTicketStatusUseCase;
import com.helpflow.supportticket.application.port.in.CreateTicketUseCase;
import com.helpflow.supportticket.application.port.in.EscalateTicketsUseCase;
import com.helpflow.supportticket.application.port.in.GetTicketUseCase;
import com.helpflow.supportticket.application.port.in.ReassignTicketUseCase;
import com.helpflow.supportticket.application.port.in.SearchTicketsUseCase;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.AssignmentResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.ChangeStatusRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.CreateTicketRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.EscalationSummaryResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.HistoryResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.PageResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.ReassignRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.TicketResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Tickets", description = "Register, consult, manage, assign and escalate IT support tickets")
@SecurityRequirement(name = "bearerAuth")
class TicketController {

    private static final Set<String> SORT_FIELDS = Set.of("id", "createdAt", "slaDueAt", "priority", "status");

    private final CreateTicketUseCase createTicket;
    private final GetTicketUseCase getTicket;
    private final SearchTicketsUseCase searchTickets;
    private final ChangeTicketStatusUseCase changeStatus;
    private final ReassignTicketUseCase reassignTicket;
    private final EscalateTicketsUseCase escalateTickets;
    private final AuthenticatedActor actors;
    private final WebMapper mapper;

    TicketController(CreateTicketUseCase createTicket, GetTicketUseCase getTicket,
            SearchTicketsUseCase searchTickets, ChangeTicketStatusUseCase changeStatus,
            ReassignTicketUseCase reassignTicket, EscalateTicketsUseCase escalateTickets,
            AuthenticatedActor actors, WebMapper mapper) {
        this.createTicket = createTicket;
        this.getTicket = getTicket;
        this.searchTickets = searchTickets;
        this.changeStatus = changeStatus;
        this.reassignTicket = reassignTicket;
        this.escalateTickets = escalateTickets;
        this.actors = actors;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Register a ticket (USUARIO, ADMIN)",
            description = """
                    Creates a ticket in **ABIERTO**, calculates its **SLA deadline** from the priority and tries the \
                    automatic assignment, all in one transaction.

                    **Flow**
                    1. The requester must be an **ACTIVO** user (`422` `user-inactive` otherwise).
                    2. `slaDueAt = now + resolutionHours` of the priority (see `GET /sla-policies`).
                    3. `technician-dispatch-service` is asked for a suggested technician and estimate. If it is down \
                    or slow (1 s connect / 2 s read timeout) the internal rules are used instead.
                    4. Internal rules: the technician must be ACTIVO, match the category (specialty equal, or generalist \
                    `OTRO`) and have capacity (`maxActiveTickets`; CRITICA gets one extra slot). The dispatch suggestion \
                    wins if it is eligible and its estimate fits the SLA window; otherwise the eligible technician from the \
                    same site with the lowest workload is chosen.
                    5. The ticket becomes **ASIGNADO**, an assignment row and history entries are written. With no eligible \
                    technician it stays **ABIERTO** (history `ASSIGNMENT_PENDING`) until an admin assigns it.

                    **Test cases**
                    | Case | Expected |
                    |---|---|
                    | Valid body as USUARIO | `201`, status `ASIGNADO` (or `ABIERTO` if nobody is eligible) |
                    | dispatch-service stopped | `201` still assigned, `GET /tickets/{id}/assignments` shows `source: RULES` |
                    | Missing title / unknown category | `400` ProblemDetail with `errors[]` |
                    | Token of a TECNICO | `403` |
                    | INACTIVO requester | `422` |
                    """)
    @ApiResponse(responseCode = "201", description = "Ticket registered")
    @ApiResponse(responseCode = "422", description = "The requester is INACTIVO")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return mapper.toResponse(createTicket.create(actors.current(), new CreateTicketUseCase.Command(
                request.title(), request.description(), request.category(), request.priority(), request.site(),
                request.affectedAsset())));
    }

    @Operation(
            summary = "List tickets visible to the caller",
            description = """
                    Paginated and filterable. **Visibility depends on the role:** USUARIO sees only their own \
                    tickets, TECNICO only the tickets assigned to them, ADMIN everything (and may filter by \
                    `requesterId` / `technicianId`).

                    Filters: `status`, `priority`, `category`, `overdue=true` (SLA running and deadline passed). \
                    Sort with `sort=field,asc|desc`, field in `id, createdAt, slaDueAt, priority, status` \
                    (default `createdAt,desc`).

                    Requester and technician are fetched with the page in a single query (no N+1).

                    **Test cases:** as USUARIO → only own tickets even if `requesterId` is another user; \
                    `?status=ASIGNADO&size=5` → at most 5 ASIGNADO tickets; `?overdue=true` as ADMIN → tickets past their deadline; \
                    `?sort=password` → `400`.
                    """)
    @GetMapping
    PageResponse<TicketResponse> list(
            @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "BAJA, MEDIA, ALTA or CRITICA") @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Category category,
            @Parameter(description = "Only unresolved tickets whose SLA deadline already passed")
            @RequestParam(defaultValue = "false") boolean overdue,
            @Parameter(description = "ADMIN only: filter by requester id") @RequestParam(required = false) Long requesterId,
            @Parameter(description = "ADMIN only: filter by assigned technician id") @RequestParam(required = false) Long technicianId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(example = "createdAt,desc") @RequestParam(required = false) String sort) {
        var result = searchTickets.search(actors.current(),
                new SearchTicketsUseCase.Filter(requesterId, technicianId, status, priority, category, overdue),
                PageParams.of(page, size, sort, SORT_FIELDS, "createdAt", true));
        return mapper.toPage(result, mapper::toResponse);
    }

    @Operation(
            summary = "Get a ticket",
            description = """
                    USUARIO: only their own ticket. TECNICO: only tickets assigned to them. ADMIN: any.

                    **Test cases:** own ticket → `200`; someone else's ticket as USUARIO → `403` `ticket-access-denied`; \
                    unknown id → `404`.
                    """)
    @ApiResponse(responseCode = "404", description = "Ticket not found")
    @GetMapping("/{id}")
    TicketResponse get(@PathVariable Long id) {
        return mapper.toResponse(getTicket.get(actors.current(), id));
    }

    @Operation(
            summary = "Ticket history (audit trail)",
            description = """
                    Chronological record of everything that happened: `CREATED`, `ASSIGNED`, `REASSIGNED`, \
                    `ASSIGNMENT_PENDING`, `STATUS_CHANGED` (with the observations given), `ESCALATED` and `SLA_BREACHED`. \
                    Same visibility as `GET /tickets/{id}`.
                    """)
    @GetMapping("/{id}/history")
    List<HistoryResponse> history(@PathVariable Long id) {
        return getTicket.history(actors.current(), id).stream().map(mapper::toResponse).toList();
    }

    @Operation(
            summary = "Assignment records of a ticket",
            description = """
                    Every assignment the ticket had, oldest first. Exactly one has `active: true` (the current owner). \
                    `source` tells how it was chosen: `DISPATCH`, `RULES` or `MANUAL`.
                    """)
    @GetMapping("/{id}/assignments")
    List<AssignmentResponse> assignments(@PathVariable Long id) {
        return getTicket.assignments(actors.current(), id).stream().map(mapper::toResponse).toList();
    }

    @Operation(
            summary = "Change the status of a ticket (TECNICO, ADMIN)",
            description = """
                    Applies a state transition and stores the mandatory `observations` in the history.

                    **Allowed transitions**
                    ```
                    ABIERTO      -> ASIGNADO*, CANCELADO
                    ASIGNADO     -> EN_PROGRESO, CANCELADO
                    EN_PROGRESO  -> ASIGNADO*, RESUELTO, CANCELADO
                    RESUELTO     -> EN_PROGRESO (reopen), CERRADO
                    CERRADO, CANCELADO: final
                    * ASIGNADO is reached only through PUT /tickets/{id}/assignment
                    ```
                    **Who:** a TECNICO can act only on tickets assigned to them and cannot cancel; an ADMIN can do any \
                    valid transition. The first move to EN_PROGRESO records `firstResponseAt`; RESUELTO records \
                    `resolvedAt` and flags the SLA as breached if it happened after `slaDueAt`.

                    **Test cases**
                    | Case | Expected |
                    |---|---|
                    | ASIGNADO -> EN_PROGRESO by the assigned technician | `200` |
                    | ASIGNADO -> RESUELTO (skipping a step) | `409` `invalid-transition` |
                    | Another technician's ticket | `403` `ticket-manage-denied` |
                    | Technician cancelling | `403` `cancel-admin-only` |
                    | Missing `observations`, or cancel with a reason shorter than 15 chars | `400` |
                    """)
    @ApiResponse(responseCode = "409", description = "Transition not allowed from the current status")
    @PatchMapping("/{id}/status")
    TicketResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return mapper.toResponse(changeStatus.changeStatus(actors.current(), id, request.status(),
                request.observations()));
    }

    @Operation(
            summary = "Assign or reassign a ticket (ADMIN)",
            description = """
                    Gives the ticket to a technician chosen by the admin. Also the way to assign tickets that stayed \
                    ABIERTO. The previous assignment is kept as inactive, the ticket goes (back) to ASIGNADO and the \
                    history records `ASSIGNED` or `REASSIGNED`. Ticket, assignment and history are written in one transaction.

                    **Validation (all `422`):** the technician must be ACTIVO (`technician-inactive`), able to handle the \
                    category (`technician-incompatible`), below their capacity (`technician-at-capacity`) and not already \
                    the owner (`already-assigned`). Closed or cancelled tickets cannot be assigned (`ticket-not-assignable`).

                    **Test cases:** compatible technician → `200`, `technician.id` changed; hardware technician for a RED ticket \
                    → `422`; USUARIO/TECNICO token → `403`.
                    """)
    @ApiResponse(responseCode = "404", description = "Ticket or technician not found")
    @ApiResponse(responseCode = "422", description = "Technician inactive, incompatible, at capacity or already assigned")
    @PutMapping("/{id}/assignment")
    TicketResponse reassign(@PathVariable Long id, @Valid @RequestBody ReassignRequest request) {
        return mapper.toResponse(reassignTicket.reassign(actors.current(), id, request.technicianId(),
                request.observations()));
    }

    @Operation(
            summary = "Run the SLA review now (ADMIN)",
            description = """
                    The same review a scheduler runs every minute (`app.sla.review-interval`), triggered on demand \
                    so it can be demonstrated: tickets that consumed the escalation threshold of their SLA window \
                    (default 80 %) are **escalated** (priority +1 level, `escalated: true`, history `ESCALATED`, metric \
                    `helpflow_tickets_escalated_total`) and tickets past their deadline are flagged \
                    **`slaBreached`** (history `SLA_BREACHED`, metric `helpflow_tickets_sla_breached_total`).

                    **Test case:** create a ticket, lower its SLA with `PUT /sla-policies/{priority}` (e.g. 1 hour is the minimum) \
                    or wait, then call this endpoint and read `GET /tickets/{id}/history`.
                    """)
    @PostMapping("/escalations")
    EscalationSummaryResponse runEscalation() {
        var summary = escalateTickets.escalateDueTickets();
        return new EscalationSummaryResponse(summary.reviewed(), summary.escalated(), summary.breached(),
                summary.failed());
    }
}
