package com.helpflow.supportticket.infrastructure.adapter.in.web;

import com.helpflow.supportticket.application.model.UserSearchCriteria;
import com.helpflow.supportticket.application.port.in.ManageUserUseCase;
import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.UserStatus;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.ChangeUserStatusRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.PageResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.UpdateProfileRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.UserResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User profiles. Everybody manages their own; admins manage everybody")
@SecurityRequirement(name = "bearerAuth")
class UserController {

    private static final Set<String> SORT_FIELDS = Set.of("id", "fullName", "email", "site");

    private final ManageUserUseCase manageUsers;
    private final AuthenticatedActor actors;
    private final WebMapper mapper;

    UserController(ManageUserUseCase manageUsers, AuthenticatedActor actors, WebMapper mapper) {
        this.manageUsers = manageUsers;
        this.actors = actors;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Current user profile",
            description = "Returns the profile of the user that owns the token. Handy to check who you are logged in as.")
    @GetMapping("/me")
    UserResponse me() {
        var actor = actors.current();
        return mapper.toResponse(manageUsers.get(actor, actor.userId()));
    }

    @Operation(
            summary = "List users (ADMIN)",
            description = """
                    Paginated list, filterable by `status`, `role` and `site`. Sort with `sort=field,asc|desc` \
                    where field is one of `id`, `fullName`, `email`, `site`.

                    **Test cases:** as ADMIN → `200` with `content[]`; as USUARIO or TECNICO → `403`; without token → `401`.
                    """)
    @GetMapping
    PageResponse<UserResponse> list(
            @Parameter(description = "ACTIVO or INACTIVO") @RequestParam(required = false) UserStatus status,
            @Parameter(description = "ROLE_USUARIO, ROLE_TECNICO or ROLE_ADMIN") @RequestParam(required = false) Role role,
            @Parameter(description = "Exact site, case-insensitive") @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(example = "fullName,asc") @RequestParam(required = false) String sort) {
        var result = manageUsers.list(new UserSearchCriteria(status, role, site),
                PageParams.of(page, size, sort, SORT_FIELDS, "id", false));
        return mapper.toPage(result, mapper::toResponse);
    }

    @Operation(
            summary = "Get a user",
            description = """
                    Admins can read any user; everybody else only their own record.

                    **Test cases:** own id → `200`; another user's id as USUARIO → `403` `user-access-denied`; \
                    unknown id as ADMIN → `404`.
                    """)
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{id}")
    UserResponse get(@PathVariable Long id) {
        return mapper.toResponse(manageUsers.get(actors.current(), id));
    }

    @Operation(
            summary = "Edit basic information",
            description = """
                    Updates full name, site and area. Document and e-mail are immutable. Admins can edit anyone; \
                    other users only themselves.
                    """)
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/{id}")
    UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProfileRequest request) {
        return mapper.toResponse(manageUsers.updateProfile(actors.current(), id,
                new ManageUserUseCase.ProfileCommand(request.fullName(), request.site(), request.area())));
    }

    @Operation(
            summary = "Activate or deactivate a user (ADMIN)",
            description = """
                    `INACTIVO` users cannot log in and cannot register tickets (`422` `user-inactive`). Existing \
                    tokens keep working until they expire, but ticket creation re-checks the status in the database.

                    **Rules:** an admin cannot deactivate their own account (`422` `cannot-deactivate-self`); when the user \
                    is a technician, the support profile follows the account status, so an inactive technician receives no \
                    new tickets.
                    """)
    @ApiResponse(responseCode = "404", description = "User not found")
    @PatchMapping("/{id}/status")
    UserResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeUserStatusRequest request) {
        return mapper.toResponse(manageUsers.changeStatus(actors.current(), id, request.status()));
    }
}
