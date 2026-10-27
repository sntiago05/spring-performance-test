package com.helpflow.supportticket.infrastructure.adapter.in.web;

import com.helpflow.supportticket.application.port.in.ManageSlaPolicyUseCase;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.SlaPolicyResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.UpdateSlaPolicyRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sla-policies")
@Tag(name = "SLA policies", description = "Resolution window and escalation threshold per priority")
@SecurityRequirement(name = "bearerAuth")
class SlaPolicyController {

    private final ManageSlaPolicyUseCase policies;
    private final WebMapper mapper;

    SlaPolicyController(ManageSlaPolicyUseCase policies, WebMapper mapper) {
        this.policies = policies;
        this.mapper = mapper;
    }

    @Operation(
            summary = "List SLA policies",
            description = """
                    Any authenticated user can read them. Defaults: CRITICA 4 h, ALTA 8 h, MEDIA 24 h, BAJA 72 h, \
                    escalating at 80 % of the window. The SLA deadline of a ticket is `createdAt + resolutionHours` \
                    (calendar hours).
                    """)
    @GetMapping
    List<SlaPolicyResponse> list() {
        return policies.list().stream().map(mapper::toResponse).toList();
    }

    @Operation(
            summary = "Configure the SLA of a priority (ADMIN)",
            description = """
                    Changes the window and escalation threshold of one priority. It applies to tickets created \
                    afterwards; existing tickets keep their original deadline.

                    **Test cases:** ADMIN with `{"resolutionHours": 6, "escalationThresholdPercent": 75}` → `200`; \
                    `resolutionHours: 0` → `400`; non-admin → `403`.
                    """)
    @PutMapping("/{priority}")
    SlaPolicyResponse update(@PathVariable Priority priority, @Valid @RequestBody UpdateSlaPolicyRequest request) {
        return mapper.toResponse(policies.update(priority, request.resolutionHours(),
                request.escalationThresholdPercent()));
    }
}
