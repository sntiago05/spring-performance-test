package com.helpflow.supportticket.application.port.out;

import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import java.util.List;

public interface TicketHistoryPort {

    TicketHistoryEntry append(TicketHistoryEntry entry);

    List<TicketHistoryEntry> findByTicketId(Long ticketId);
}
