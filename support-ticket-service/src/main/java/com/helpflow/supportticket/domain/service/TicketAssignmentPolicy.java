package com.helpflow.supportticket.domain.service;

import com.helpflow.supportticket.domain.model.AssignmentSelection;
import com.helpflow.supportticket.domain.model.AssignmentSource;
import com.helpflow.supportticket.domain.model.DispatchSuggestion;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.Ticket;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Internal assignment rules. A technician is eligible when they are active, their specialty matches
 * the ticket category and they have capacity for the ticket priority. The external dispatch
 * suggestion goes first when the technician is eligible and the estimate fits in the SLA window;
 * the remaining eligible technicians follow ordered by same site, lowest workload and code.
 */
public final class TicketAssignmentPolicy {

    public record Candidate(Technician technician, int activeTickets) {
    }

    public List<AssignmentSelection> rank(Ticket ticket, DispatchSuggestion suggestion, List<Candidate> candidates,
            Instant now) {
        List<Candidate> eligible = candidates.stream()
                .filter(candidate -> isEligible(ticket, candidate))
                .sorted(Comparator
                        .comparing((Candidate c) -> !c.technician().isOnSite(ticket.site()))
                        .thenComparingInt(Candidate::activeTickets)
                        .thenComparing(c -> c.technician().code()))
                .toList();

        long hoursRemaining = Math.max(0, Duration.between(now, ticket.slaDueAt()).toHours());
        AssignmentSelection suggested = suggestion == null ? null : eligible.stream()
                .filter(c -> c.technician().code().equals(suggestion.technicianCode()))
                .filter(c -> suggestion.estimatedHours() <= hoursRemaining)
                .findFirst()
                .map(c -> new AssignmentSelection(c.technician(), AssignmentSource.DISPATCH,
                        suggestion.estimatedHours(), suggestion.availability(), true))
                .orElse(null);

        return Stream.concat(
                        Stream.ofNullable(suggested),
                        eligible.stream()
                                .filter(c -> suggested == null || !c.technician().equals(suggested.technician()))
                                .map(c -> new AssignmentSelection(c.technician(), AssignmentSource.RULES,
                                        null, null, null)))
                .toList();
    }

    public boolean isEligible(Ticket ticket, Candidate candidate) {
        Technician technician = candidate.technician();
        return technician.isActive()
                && technician.canHandle(ticket.category())
                && technician.hasCapacityFor(ticket.priority(), candidate.activeTickets());
    }
}
