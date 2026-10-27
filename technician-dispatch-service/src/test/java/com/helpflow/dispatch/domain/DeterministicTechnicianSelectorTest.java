package com.helpflow.dispatch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpflow.dispatch.domain.model.Availability;
import com.helpflow.dispatch.domain.model.Category;
import com.helpflow.dispatch.domain.model.Priority;
import com.helpflow.dispatch.domain.service.DeterministicTechnicianSelector;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DeterministicTechnicianSelectorTest {

    private final DeterministicTechnicianSelector selector = new DeterministicTechnicianSelector(15);

    @Test
    void sameInputAlwaysReturnsSameSuggestion() {
        var first = selector.select(125, Category.RED, Priority.ALTA);
        var second = new DeterministicTechnicianSelector(15).select(125, Category.RED, Priority.ALTA);

        assertThat(second).isEqualTo(first);
        assertThat(first.ticketId()).isEqualTo(125);
    }

    @Test
    void suggestionStaysInsideThePoolAndEstimateRange() {
        for (long ticketId = 1; ticketId <= 500; ticketId++) {
            for (Category category : Category.values()) {
                for (Priority priority : Priority.values()) {
                    var suggestion = selector.select(ticketId, category, priority);

                    int number = Integer.parseInt(suggestion.technicianId().substring("TEC-".length()));
                    assertThat(number).isBetween(1, 15);
                    assertThat(suggestion.estimatedHours()).isBetween(0, 12);
                    assertThat(suggestion.availability())
                            .isEqualTo(Availability.fromEstimatedHours(suggestion.estimatedHours()));
                }
            }
        }
    }

    @Test
    void differentCombinationsProduceDifferentTechniciansAndEstimates() {
        Set<String> technicians = new HashSet<>();
        Set<Integer> estimates = new HashSet<>();
        for (long ticketId = 1; ticketId <= 100; ticketId++) {
            var suggestion = selector.select(ticketId, Category.HARDWARE, Priority.MEDIA);
            technicians.add(suggestion.technicianId());
            estimates.add(suggestion.estimatedHours());
        }

        assertThat(technicians.size()).isGreaterThan(5);
        assertThat(estimates.size()).isGreaterThan(5);
    }

    @Test
    void rejectsEmptyPool() {
        assertThatThrownBy(() -> new DeterministicTechnicianSelector(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
