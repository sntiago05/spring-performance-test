package com.helpflow.supportticket.infrastructure.adapter.in.web;

import com.helpflow.supportticket.application.port.in.AuthenticateUserUseCase;
import com.helpflow.supportticket.application.port.in.RegisterUserUseCase;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.LoginRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.LoginResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.UserResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.mapper.WebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Public endpoints: create an account and obtain a JWT")
class AuthController {

    private final RegisterUserUseCase registerUser;
    private final AuthenticateUserUseCase authenticateUser;
    private final WebMapper mapper;

    AuthController(RegisterUserUseCase registerUser, AuthenticateUserUseCase authenticateUser, WebMapper mapper) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Register a new user",
            description = """
                    Creates an **ACTIVO** account with role **ROLE_USUARIO**. Technicians and admins cannot be \
                    self-registered (admins create technicians with `POST /technicians`).

                    **Rules:** the e-mail must be a valid *corporate* address (public providers such as gmail.com or \
                    hotmail.com are rejected); document and e-mail must be unique; the password is stored hashed \
                    with BCrypt and is never returned.

                    **How to use:** send the body below, then call `POST /auth/login` with the same e-mail and password.

                    **Test cases**
                    | Case | Expected |
                    |---|---|
                    | Valid body | `201` with the user (no password) |
                    | Same e-mail or document again | `409` ProblemDetail `duplicate-email` / `duplicate-document` |
                    | `x@gmail.com` | `400` validation error (`email`) |
                    | Password shorter than 8 | `400` validation error (`password`) |
                    """)
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Invalid fields")
    @ApiResponse(responseCode = "409", description = "Document or e-mail already registered")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return mapper.toResponse(registerUser.register(new RegisterUserUseCase.Command(request.document(),
                request.fullName(), request.email(), request.password(), request.site(), request.area())));
    }

    @Operation(
            summary = "Log in and obtain a JWT",
            description = """
                    Validates the credentials and returns a signed, stateless **JWT** (HS256).

                    **How to use:** copy `accessToken`, press **Authorize** in Swagger UI and paste it (without the \
                    `Bearer` prefix); other clients send `Authorization: Bearer <token>`. The token carries the \
                    e-mail (`sub`), the user id (`uid`) and the role (`roles`), and expires at `expiresAt`.

                    **Test cases**
                    | Case | Expected |
                    |---|---|
                    | Correct credentials | `200` with `accessToken` |
                    | Wrong password or unknown e-mail | `401` `bad-credentials` (same answer for both, no user enumeration) |
                    | Account set to INACTIVO | `403` `account-inactive` |

                    Every failed attempt increments the metric `helpflow_auth_failures_total`.
                    """)
    @ApiResponse(responseCode = "200", description = "Authenticated")
    @ApiResponse(responseCode = "401", description = "Bad credentials")
    @ApiResponse(responseCode = "403", description = "Inactive account")
    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var result = authenticateUser.login(new AuthenticateUserUseCase.Command(request.email(), request.password()));
        return new LoginResponse(result.token().value(), "Bearer", result.token().expiresAt(),
                mapper.toResponse(result.user()));
    }
}
