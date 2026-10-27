package com.helpflow.supportticket.application.port.out;

import com.helpflow.supportticket.domain.exception.TechnicianDispatchUnavailableException;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.DispatchSuggestion;
import com.helpflow.supportticket.domain.model.Priority;

/**
 * Outbound port to the technician-dispatch microservice.
 */
public interface TechnicianDispatchPort {

    /**
     * @throws TechnicianDispatchUnavailableException when the remote service fails, times out or
     *                                                answers something unusable
     */
    DispatchSuggestion suggest(Long ticketId, Category category, Priority priority);
}
