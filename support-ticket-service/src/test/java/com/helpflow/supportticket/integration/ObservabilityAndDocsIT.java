package com.helpflow.supportticket.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpflow.supportticket.support.AbstractIntegrationTest;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class ObservabilityAndDocsIT extends AbstractIntegrationTest {

    private int status(String path, String token) throws Exception {
        return mockMvc.perform(authed(get(path), token)).andReturn().getResponse().getStatus();
    }

    @Test
    void healthInfoAndPrometheusArePublic() throws Exception {
        assertThat(status("/actuator/health", null)).isEqualTo(200);
        assertThat(status("/actuator/health/liveness", null)).isEqualTo(200);
        assertThat(status("/actuator/health/readiness", null)).isEqualTo(200);
        assertThat(status("/actuator/info", null)).isEqualTo(200);
        assertThat(status("/actuator/prometheus", null)).isEqualTo(200);
        String health = mockMvc.perform(get("/actuator/health")).andReturn().getResponse().getContentAsString();
        assertThat(health).contains("\"status\":\"UP\"").doesNotContain("db").doesNotContain("diskSpace");
    }

    @Test
    void healthDetailsAndMetricsAreRestrictedToAdmins() throws Exception {
        String user = newUserToken();
        String admin = adminToken();

        assertThat(status("/actuator/metrics", null)).isEqualTo(401);
        assertThat(status("/actuator/metrics", user)).isEqualTo(403);
        assertThat(status("/actuator/metrics", admin)).isEqualTo(200);
        assertThat(status("/actuator/metrics/jvm.memory.used", admin)).isEqualTo(200);
        String details = mockMvc.perform(authed(get("/actuator/health"), admin)).andReturn().getResponse()
                .getContentAsString();
        assertThat(details).contains("\"db\"");
        // endpoints that are not exposed do not exist, even for admins
        assertThat(status("/actuator/env", admin)).isEqualTo(404);
        assertThat(status("/actuator/heapdump", admin)).isEqualTo(404);
    }

    @Test
    void prometheusExposesHttpJvmAndApplicationLabelledSeries() throws Exception {
        getJson("/tickets", null); // generates a 401 http.server.requests sample
        postJson("/auth/login", null, Map.of("email", "nobody@helpflow.example", "password", "wrong-password"));

        String metrics = mockMvc.perform(get("/actuator/prometheus")).andReturn().getResponse().getContentAsString();

        assertThat(metrics).contains("jvm_memory_used_bytes{application=\"support-ticket-service\"");
        assertThat(metrics).contains("process_cpu_usage{application=\"support-ticket-service\"");
        assertThat(metrics).containsPattern("http_server_requests_seconds_count\\{[^}]*status=\"401\"[^}]*\\}");
        assertThat(metrics).contains("http_server_requests_seconds_bucket");
        assertThat(metrics).contains("hikaricp_connections_active");
    }

    @Test
    void everyEndpointIsDocumentedInTheOpenApiContract() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode api = json(result);

        assertThat(api.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(api.at("/info/description").asText()).contains("Quick start", "Roles");
        Iterator<Map.Entry<String, JsonNode>> paths = api.get("paths").fields();
        int operations = 0;
        while (paths.hasNext()) {
            var path = paths.next();
            Iterator<Map.Entry<String, JsonNode>> methods = path.getValue().fields();
            while (methods.hasNext()) {
                var method = methods.next();
                String label = method.getKey().toUpperCase() + " " + path.getKey();
                JsonNode operation = method.getValue();
                assertThat(operation.path("summary").asText()).as(label + " summary").isNotBlank();
                assertThat(operation.path("tags")).as(label + " tags").isNotEmpty();
                assertThat(operation.path("responses").size()).as(label + " responses").isGreaterThanOrEqualTo(2);
                operations++;
            }
        }
        assertThat(operations).isGreaterThanOrEqualTo(21);
        assertThat(api.at("/paths/~1tickets/post/description").asText()).contains("Test cases");
        assertThat(api.at("/paths/~1auth~1login/post/description").asText()).contains("Test cases");
        assertThat(api.at("/paths/~1tickets/post/security/0/bearerAuth").isArray()).isTrue();
        assertThat(api.at("/paths/~1auth~1login/post/security").isMissingNode()).isTrue();
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        assertThat(status("/swagger-ui/index.html", null)).isEqualTo(200);
        assertThat(mockMvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus()).isIn(200, 302);
    }

    @Test
    void errorsAreLocalizedAccordingToAcceptLanguage() throws Exception {
        String user = newUserToken();

        MvcResult english = mockMvc.perform(get("/tickets/999999").header("Authorization", "Bearer " + user))
                .andReturn();
        MvcResult spanish = mockMvc.perform(get("/tickets/999999").header("Authorization", "Bearer " + adminToken())
                .header("Accept-Language", "es-CO,es;q=0.9")).andReturn();
        MvcResult spanishValidation = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/tickets").header("Authorization", "Bearer " + user).header("Accept-Language", "es")
                .contentType("application/json").content("{}")).andReturn();
        MvcResult spanishUnauthorized = mockMvc.perform(get("/tickets").header("Accept-Language", "es")).andReturn();
        MvcResult unsupportedLanguage = mockMvc.perform(get("/tickets").header("Accept-Language", "fr")).andReturn();

        assertThat(english.getResponse().getStatus()).isEqualTo(404);
        assertThat(json(english).get("title").asText()).isEqualTo("Ticket not found");
        assertThat(json(english).get("detail").asText()).isEqualTo("Ticket 999999 does not exist.");
        assertThat(json(spanish).get("title").asText()).isEqualTo("Ticket no encontrado");
        assertThat(json(spanish).get("detail").asText()).isEqualTo("El ticket 999999 no existe.");
        assertThat(spanishValidation.getResponse().getStatus()).isEqualTo(400);
        JsonNode validation = json(spanishValidation);
        assertThat(validation.get("title").asText()).isEqualTo("Validación fallida");
        assertThat(validation.get("errors").findValuesAsText("message")).contains("es obligatorio");
        assertThat(json(spanishUnauthorized).get("title").asText()).isEqualTo("Autenticación requerida");
        // unsupported languages fall back to English
        assertThat(json(unsupportedLanguage).get("title").asText()).isEqualTo("Authentication required");
    }
}
