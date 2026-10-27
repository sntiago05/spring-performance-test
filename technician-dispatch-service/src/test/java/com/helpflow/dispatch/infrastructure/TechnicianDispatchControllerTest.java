package com.helpflow.dispatch.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class TechnicianDispatchControllerTest {

    private static final String VALID_BODY = """
            {"ticketId": 125, "categoria": "RED", "prioridad": "ALTA"}""";

    @Autowired
    MockMvc mockMvc;

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
                .andExpect(jsonPath("$.title").value("Malformed request"));
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
}
