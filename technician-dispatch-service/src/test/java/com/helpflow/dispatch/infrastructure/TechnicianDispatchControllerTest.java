package com.helpflow.dispatch.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpflow.dispatch.application.exception.DispatchApplicationException;
import com.helpflow.dispatch.application.port.in.SuggestTechnicianUseCase;
import com.helpflow.dispatch.domain.exception.DomainConflictException;
import com.helpflow.dispatch.domain.exception.DomainRuleViolationException;
import com.helpflow.dispatch.infrastructure.exception.DispatchInfrastructureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class TechnicianDispatchControllerTest {

    private static final String VALID_BODY = """
            {"ticketId": 125, "categoria": "RED", "prioridad": "ALTA"}""";

    @Autowired
    MockMvc mockMvc;

    @MockitoSpyBean
    SuggestTechnicianUseCase suggestTechnician;

    @AfterEach
    void tearDown() {
        reset(suggestTechnician);
    }

    private ResultActions dispatch(String body, String language) throws Exception {
        return mockMvc.perform(post("/technician-dispatch")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", language)
                .content(body));
    }

    @Test
    void returnsTheSpecifiedResponseShape() throws Exception {
        dispatch(VALID_BODY, "en")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(125))
                .andExpect(jsonPath("$.tecnicoId").value(org.hamcrest.Matchers.matchesPattern("TEC-\\d{3}")))
                .andExpect(jsonPath("$.disponibilidad").exists())
                .andExpect(jsonPath("$.tiempoEstimadoHoras").isNumber());
    }

    @Test
    void sameRequestReturnsSameBody() throws Exception {
        String first = dispatch(VALID_BODY, "en").andReturn().getResponse().getContentAsString();
        String second = dispatch(VALID_BODY, "en").andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
    }

    @Test
    void acceptsCriticalPriorityWithAccentAndAnyCase() throws Exception {
        dispatch("""
                {"ticketId": 7, "categoria": "hardware", "prioridad": "CRÍTICA"}""", "en")
                .andExpect(status().isOk());
    }

    @Test
    void invalidFieldsProduceLocalizedProblemDetail() throws Exception {
        dispatch("""
                {"ticketId": -5, "categoria": "RED", "prioridad": "ALTA"}""", "es")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Validación fallida"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:validation-error"))
                .andExpect(jsonPath("$.detail").value("La solicitud contiene campos inválidos."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.errors[0].field").value("ticketId"));
    }

    @Test
    void unknownEnumValueIsABadRequestInEnglishByDefault() throws Exception {
        dispatch("""
                {"ticketId": 1, "categoria": "NOPE", "prioridad": "ALTA"}""", "en")
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:malformed-request"))
                .andExpect(jsonPath("$.title").value("Malformed request"))
                .andExpect(jsonPath("$.detail").value(
                        "The request body is missing or contains an invalid value (check categoria and prioridad)."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void malformedJsonSyntaxReturnsProblemDetail400() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:malformed-request"))
                .andExpect(jsonPath("$.title").value("Malformed request"))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void methodNotAllowedReturnsProblemDetail405() throws Exception {
        mockMvc.perform(get("/technician-dispatch")
                        .header("Accept-Language", "es"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:method-not-allowed"))
                .andExpect(jsonPath("$.title").value("Método no permitido"))
                .andExpect(jsonPath("$.detail").value("El método HTTP no está soportado para este endpoint."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unsupportedMediaTypeReturnsProblemDetail415() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text payload")
                        .header("Accept-Language", "en"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:unsupported-media-type"))
                .andExpect(jsonPath("$.title").value("Unsupported media type"))
                .andExpect(jsonPath("$.detail").value("The requested media type is not supported."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void notAcceptableReturnsProblemDetail406() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_XML)
                        .content(VALID_BODY)
                        .header("Accept-Language", "es"))
                .andExpect(status().isNotAcceptable())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(406))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:not-acceptable"))
                .andExpect(jsonPath("$.title").value("No aceptable"))
                .andExpect(jsonPath("$.detail").value(
                        "La representación solicitada no se puede servir en el formato aceptado."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void resourceNotFoundReturnsProblemDetail404() throws Exception {
        mockMvc.perform(get("/non-existent-resource")
                        .header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:resource-not-found"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("The requested resource or endpoint was not found."))
                .andExpect(jsonPath("$.instance").value("/non-existent-resource"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void domainRuleViolationReturnsProblemDetail422() throws Exception {
        doThrow(new DomainRuleViolationException("Domain rule broken"))
                .when(suggestTechnician).suggest(any());

        dispatch(VALID_BODY, "es")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:domain-rule-violation"))
                .andExpect(jsonPath("$.title").value("Entidad no procesable"))
                .andExpect(jsonPath("$.detail").value("La solicitud viola una regla de dominio."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void domainConflictReturnsProblemDetail409() throws Exception {
        doThrow(new DomainConflictException("State conflict"))
                .when(suggestTechnician).suggest(any());

        dispatch(VALID_BODY, "en")
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:state-conflict"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("The request conflicts with the current state of the resource."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void applicationExceptionReturnsProblemDetail422() throws Exception {
        doThrow(new DispatchApplicationException("Use case could not complete"))
                .when(suggestTechnician).suggest(any());

        dispatch(VALID_BODY, "es")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:application-error"))
                .andExpect(jsonPath("$.title").value("Error de aplicación"))
                .andExpect(jsonPath("$.detail").value("La operación solicitada no pudo completarse."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void infrastructureExceptionReturnsProblemDetail500() throws Exception {
        doThrow(new DispatchInfrastructureException("Adapter connection dropped"))
                .when(suggestTechnician).suggest(any());

        dispatch(VALID_BODY, "en")
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:infrastructure-error"))
                .andExpect(jsonPath("$.title").value("Infrastructure error"))
                .andExpect(jsonPath("$.detail").value("An infrastructure or configuration error occurred."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Adapter"))));
    }

    @Test
    void unexpectedErrorReturnsProblemDetail500WithoutLeakingInternalDetails() throws Exception {
        doThrow(new RuntimeException("SQL syntax error near SELECT * FROM secret_passwords"))
                .when(suggestTechnician).suggest(any());

        dispatch(VALID_BODY, "en")
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.type").value("urn:helpflow:problem:internal-error"))
                .andExpect(jsonPath("$.title").value("Internal error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Quote the traceId when reporting it."))
                .andExpect(jsonPath("$.instance").value("/technician-dispatch"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SQL"))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void traceIdHeaderIsPropagatedToTheResponse() throws Exception {
        mockMvc.perform(post("/technician-dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "abc123")
                        .content(VALID_BODY))
                .andExpect(header().string("X-Trace-Id", "abc123"));
    }

    @Test
    void healthAndPrometheusAreExposed() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }

    @Test
    void openApiDocumentationExposesNewRouteAndErrorResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['405']").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['406']").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['415']").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['400'].content['application/problem+json']").exists())
                .andExpect(jsonPath("$.paths['/technician-dispatch'].post.responses['500'].content['application/problem+json']").exists());
    }
}
