package com.helpflow.supportticket.infrastructure.adapter.out.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.helpflow.supportticket.domain.exception.TechnicianDispatchUnavailableException;
import com.helpflow.supportticket.domain.model.Availability;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The REST adapter against a simulated technician-dispatch-service: request contract, response mapping and
 * every way the remote call can fail.
 */
class RestTechnicianDispatchAdapterTest {

    private MockRestServiceServer server;
    private RestTechnicianDispatchAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://dispatch.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new RestTechnicianDispatchAdapter(builder.build());
    }

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    @Test
    void sendsTheSpecifiedRequestAndMapsTheResponse() {
        MDC.put("traceId", "trace-123");
        server.expect(requestTo("http://dispatch.test/technician-dispatch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Trace-Id", "trace-123"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketId").value(125))
                .andExpect(jsonPath("$.categoria").value("RED"))
                .andExpect(jsonPath("$.prioridad").value("ALTA"))
                .andRespond(withSuccess("""
                        {"ticketId": 125, "tecnicoId": "TEC-014", "disponibilidad": "MEDIA", "tiempoEstimadoHoras": 4}""",
                        MediaType.APPLICATION_JSON));

        var suggestion = adapter.suggest(125L, Category.RED, Priority.ALTA);

        assertThat(suggestion.technicianCode()).isEqualTo("TEC-014");
        assertThat(suggestion.availability()).isEqualTo(Availability.MEDIA);
        assertThat(suggestion.estimatedHours()).isEqualTo(4);
        server.verify();
    }

    @Test
    void serverErrorsAreReportedAsUnavailable() {
        server.expect(requestTo("http://dispatch.test/technician-dispatch")).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.suggest(1L, Category.RED, Priority.ALTA))
                .isInstanceOf(TechnicianDispatchUnavailableException.class);
    }

    @Test
    void networkFailuresAreReportedAsUnavailable() {
        server.expect(requestTo("http://dispatch.test/technician-dispatch"))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> adapter.suggest(1L, Category.RED, Priority.ALTA))
                .isInstanceOf(TechnicianDispatchUnavailableException.class);
    }

    @Test
    void incompleteOrUnrecognizedPayloadsAreReportedAsUnavailable() {
        server.expect(requestTo("http://dispatch.test/technician-dispatch"))
                .andRespond(withSuccess("{\"ticketId\": 1}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://dispatch.test/technician-dispatch"))
                .andRespond(withSuccess("""
                        {"ticketId": 1, "tecnicoId": "TEC-001", "disponibilidad": "ENORME", "tiempoEstimadoHoras": 1}""",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.suggest(1L, Category.RED, Priority.ALTA))
                .isInstanceOf(TechnicianDispatchUnavailableException.class);
        assertThatThrownBy(() -> adapter.suggest(1L, Category.RED, Priority.ALTA))
                .isInstanceOf(TechnicianDispatchUnavailableException.class);
    }
}
