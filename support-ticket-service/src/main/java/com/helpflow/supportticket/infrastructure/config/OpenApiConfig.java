package com.helpflow.supportticket.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    OpenAPI supportTicketOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("support-ticket-service")
                        .version("1.0.0")
                        .description("""
                                HelpFlow IT support ticket service: register, prioritize, assign, escalate and close \
                                support tickets with a full audit trail.

                                ## Quick start
                                1. `POST /auth/register` and `POST /auth/login` → copy `accessToken`.
                                2. Press **Authorize** and paste the token.
                                3. `POST /tickets` → the ticket is assigned automatically (see its `technician` and \
                                `GET /tickets/{id}/history`).
                                4. Log in as a technician (`tec-001@...`) or admin to manage it. Seeded accounts exist only \
                                when the bootstrap passwords are configured (see the README).

                                ## Roles
                                | Role | Can |
                                |---|---|
                                | `ROLE_USUARIO` | create tickets, consult **only its own** |
                                | `ROLE_TECNICO` | consult and manage (status changes) **only tickets assigned to it** |
                                | `ROLE_ADMIN` | everything: reassignment, SLA configuration, technicians, users |

                                ## Errors
                                All errors are RFC 7807 `application/problem+json` with `type`, `title`, `status`, `detail`, \
                                `instance`, `timestamp` and `traceId` (also sent as `X-Trace-Id`). Messages are English by \
                                default; send `Accept-Language: es` for Spanish.

                                ## Enumerations
                                Values follow the specification: categories `HARDWARE, SOFTWARE, RED, ACCESOS, OTRO`; \
                                priorities `BAJA, MEDIA, ALTA, CRITICA` (`CRÍTICA` accepted); statuses `ABIERTO, ASIGNADO, \
                                EN_PROGRESO, RESUELTO, CERRADO, CANCELADO`.
                                """))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the `accessToken` returned by POST /auth/login")));
    }

    /**
     * Adds the answers every operation can produce (401/403 for secured ones, 500) and the
     * Accept-Language header, so they do not have to be repeated on each endpoint.
     */
    @Bean
    OperationCustomizer commonResponses() {
        return (Operation operation, org.springframework.web.method.HandlerMethod handlerMethod) -> {
            boolean secured = operation.getSecurity() != null && !operation.getSecurity().isEmpty()
                    || handlerMethod.getBeanType().isAnnotationPresent(
                            io.swagger.v3.oas.annotations.security.SecurityRequirement.class);
            if (secured) {
                addIfAbsent(operation, "401", "Missing, invalid or expired token");
                addIfAbsent(operation, "403", "The role or ownership does not allow this operation");
            }
            addIfAbsent(operation, "500", "Unexpected error (quote the traceId)");
            operation.addParametersItem(new HeaderParameter().name("Accept-Language").required(false)
                    .description("`en` (default) or `es`: language of error messages")
                    .schema(new Schema<String>().type("string").example("en")));
            return operation;
        };
    }

    private static void addIfAbsent(Operation operation, String code, String description) {
        if (operation.getResponses().containsKey(code)) {
            return;
        }
        operation.getResponses().addApiResponse(code, new ApiResponse().description(description)
                .content(new Content().addMediaType(PROBLEM_JSON, new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))));
    }
}
