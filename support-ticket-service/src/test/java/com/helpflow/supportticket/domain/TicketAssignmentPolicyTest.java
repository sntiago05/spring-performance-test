package com.helpflow.supportticket.domain;

import static com.helpflow.supportticket.support.Fixtures.NOW;
import static com.helpflow.supportticket.support.Fixtures.inactive;
import static com.helpflow.supportticket.support.Fixtures.openTicket;
import static com.helpflow.supportticket.support.Fixtures.technician;
import static org.assertj.core.api.Assertions.assertThat;

import com.helpflow.supportticket.domain.model.AssignmentSelection;
import com.helpflow.supportticket.domain.model.AssignmentSource;
import com.helpflow.supportticket.domain.model.Availability;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.DispatchSuggestion;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.service.TicketAssignmentPolicy;
import com.helpflow.supportticket.domain.service.TicketAssignmentPolicy.Candidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TicketAssignmentPolicyTest {

    private final TicketAssignmentPolicy policy = new TicketAssignmentPolicy();
    // 8 hour SLA window created at NOW
    private final Ticket redTicket = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA");

    private List<AssignmentSelection> rank(DispatchSuggestion suggestion, Candidate... candidates) {
        return policy.rank(redTicket, suggestion, List.of(candidates), NOW);
    }

    @Test
    void theDispatchSuggestionWinsWhenTheTechnicianIsEligibleAndFitsTheSla() {
        var suggested = technician(8, Category.RED, "MEDELLIN", 3);
        var sameSite = technician(7, Category.RED, "BOGOTA", 3);

        var ranking = rank(new DispatchSuggestion("TEC-008", Availability.MEDIA, 4),
                new Candidate(sameSite, 0), new Candidate(suggested, 2));

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).technician()).isEqualTo(suggested);
        assertThat(ranking.get(0).source()).isEqualTo(AssignmentSource.DISPATCH);
        assertThat(ranking.get(0).estimatedHours()).isEqualTo(4);
        assertThat(ranking.get(0).withinSla()).isTrue();
        assertThat(ranking.get(1).source()).isEqualTo(AssignmentSource.RULES);
    }

    @Test
    void aSuggestionThatCannotMeetTheSlaIsDemotedToTheRules() {
        var suggested = technician(8, Category.RED, "MEDELLIN", 3);
        var sameSite = technician(7, Category.RED, "BOGOTA", 3);

        // 12h estimate in an 8h window
        var ranking = rank(new DispatchSuggestion("TEC-008", Availability.BAJA, 12),
                new Candidate(suggested, 0), new Candidate(sameSite, 2));

        assertThat(ranking).extracting(AssignmentSelection::source).containsOnly(AssignmentSource.RULES);
        assertThat(ranking.get(0).technician()).isEqualTo(sameSite); // same site beats lower workload
    }

    @Test
    void ineligibleTechniciansAreNeverRanked() {
        var wrongSpecialty = technician(1, Category.SOFTWARE, "BOGOTA", 3);
        var disabled = inactive(technician(2, Category.RED, "BOGOTA", 3));
        var full = technician(3, Category.RED, "BOGOTA", 2);
        var available = technician(4, Category.RED, "CALI", 2);

        var ranking = rank(new DispatchSuggestion("TEC-002", Availability.ALTA, 1),
                new Candidate(wrongSpecialty, 0), new Candidate(disabled, 0), new Candidate(full, 2),
                new Candidate(available, 1));

        assertThat(ranking).extracting(s -> s.technician().code()).containsExactly("TEC-004");
    }

    @Test
    void withoutASuggestionTheRulesOrderBySiteThenWorkloadThenCode() {
        var farLight = technician(1, Category.RED, "CALI", 5);
        var nearBusy = technician(2, Category.RED, "BOGOTA", 5);
        var nearLight = technician(4, Category.RED, "BOGOTA", 5);
        var nearLightTie = technician(3, Category.RED, "BOGOTA", 5);

        var ranking = rank(null, new Candidate(farLight, 0), new Candidate(nearBusy, 3),
                new Candidate(nearLight, 1), new Candidate(nearLightTie, 1));

        assertThat(ranking).extracting(s -> s.technician().code())
                .containsExactly("TEC-003", "TEC-004", "TEC-002", "TEC-001");
        assertThat(ranking.get(0).estimatedHours()).isNull();
        assertThat(ranking.get(0).withinSla()).isNull();
    }

    @Test
    void criticalTicketsMayUseTheOverflowSlot() {
        Ticket critical = openTicket(1, Category.RED, Priority.CRITICA, "BOGOTA");
        var full = technician(1, Category.RED, "BOGOTA", 2);

        var ranking = policy.rank(critical, null, List.of(new Candidate(full, 2)), NOW);

        assertThat(ranking).hasSize(1);
        assertThat(policy.rank(redTicket, null, List.of(new Candidate(full, 2)), NOW)).isEmpty();
    }

    @Test
    void noCandidatesMeansNoAssignment() {
        assertThat(rank(new DispatchSuggestion("TEC-001", Availability.ALTA, 1))).isEmpty();
    }
}
