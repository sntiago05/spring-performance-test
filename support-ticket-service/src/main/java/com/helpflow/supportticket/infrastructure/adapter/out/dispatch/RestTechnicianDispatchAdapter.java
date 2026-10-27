package com.helpflow.supportticket.infrastructure.adapter.out.dispatch;

import com.helpflow.supportticket.application.port.out.TechnicianDispatchPort;
import com.helpflow.supportticket.domain.exception.TechnicianDispatchUnavailableException;
import com.helpflow.supportticket.domain.model.Availability;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.DispatchSuggestion;
import com.helpflow.supportticket.domain.model.Priority;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * REST adapter for POST {baseUrl}/technician-dispatch. Any transport, status or payload problem is
 * reported as {@link TechnicianDispatchUnavailableException} so the use case can fall back to its own
 * assignment rules instead of failing the ticket registration.
 */
@Component
class RestTechnicianDispatchAdapter implements TechnicianDispatchPort {

    static final String TRACE_HEADER = "X-Trace-Id";

    private final RestClient client;

    RestTechnicianDispatchAdapter(RestClient dispatchRestClient) {
        this.client = dispatchRestClient;
    }

    @Override
    public DispatchSuggestion suggest(Long ticketId, Category category, Priority priority) {
        try {
            var spec = client.post().uri("/technician-dispatch");
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                spec = spec.header(TRACE_HEADER, traceId);
            }
            DispatchResponse response = spec
                    .body(new DispatchRequest(ticketId, category.name(), priority.name()))
                    .retrieve()
                    .body(DispatchResponse.class);
            if (response == null || response.tecnicoId() == null || response.disponibilidad() == null
                    || response.tiempoEstimadoHoras() == null) {
                throw new TechnicianDispatchUnavailableException("dispatch-unavailable",
                        "technician-dispatch-service returned an incomplete response");
            }
            return new DispatchSuggestion(response.tecnicoId(), Availability.valueOf(response.disponibilidad()),
                    response.tiempoEstimadoHoras());
        } catch (RestClientException | IllegalArgumentException ex) {
            throw new TechnicianDispatchUnavailableException("dispatch-unavailable",
                    "technician-dispatch-service call failed: " + ex.getMessage());
        }
    }

    record DispatchRequest(Long ticketId, String categoria, String prioridad) {
    }

    record DispatchResponse(Long ticketId, String tecnicoId, String disponibilidad, Integer tiempoEstimadoHoras) {
    }
}
