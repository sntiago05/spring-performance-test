package com.helpflow.supportticket.application.port.out;

import com.helpflow.supportticket.domain.model.AssignmentSource;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import java.time.Duration;

/**
 * Business metrics worth observing; the adapter decides how they are published.
 */
public interface TicketMetricsPort {

    void ticketCreated(Category category, Priority priority);

    void ticketAssigned(AssignmentSource source);

    void firstResponse(Duration sinceCreation);

    void resolution(Duration sinceCreation);

    void slaBreached();

    void ticketEscalated();

    void authenticationFailed(String reason);

    void accessDenied();
}
