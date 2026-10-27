package com.helpflow.supportticket.application.port.out;

import com.helpflow.supportticket.domain.model.TicketAssignment;
import java.util.List;

public interface TicketAssignmentPort {

    TicketAssignment save(TicketAssignment assignment);

    void deactivateActiveOf(Long ticketId);

    List<TicketAssignment> findByTicketId(Long ticketId);
}
