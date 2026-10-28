package com.helpflow.dispatch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpflow.dispatch.domain.exception.DomainRuleViolationException;
import com.helpflow.dispatch.domain.model.Availability;
import com.helpflow.dispatch.domain.model.Category;
import com.helpflow.dispatch.domain.model.DispatchTechnician;
import com.helpflow.dispatch.domain.model.Priority;
import com.helpflow.dispatch.domain.service.DeterministicTechnicianSelector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeterministicTechnicianSelectorTest {

    /** The same 15 technicians seeded in support-ticket-service. */
    static List<DispatchTechnician> seededPool() {
        List<DispatchTechnician> pool = new ArrayList<>();
        Category[] specialties = {Category.HARDWARE, Category.SOFTWARE, Category.RED, Category.ACCESOS, Category.OTRO};
        for (int i = 0; i < 15; i++) {
            pool.add(new DispatchTechnician("TEC-%03d".formatted(i + 1), specialties[i / 3]));
        }
        return pool;
    }

    private final DeterministicTechnicianSelector selector = new DeterministicTechnicianSelector(seededPool());
    private final Map<String, DispatchTechnician> byCode =
            seededPool().stream().collect(Collectors.toMap(DispatchTechnician::code, t -> t));

    @Test
    void sameInputAlwaysReturnsSameSuggestion() {
        var first = selector.select(125, Category.RED, Priority.ALTA);
        var second = new DeterministicTechnicianSelector(seededPool()).select(125, Category.RED, Priority.ALTA);

        assertThat(second).isEqualTo(first);
        assertThat(first.ticketId()).isEqualTo(125);
    }

    @Test
    void theOrderOfTheConfiguredPoolDoesNotChangeTheResult() {
        List<DispatchTechnician> shuffled = seededPool();
        Collections.reverse(shuffled);

        var expected = selector.select(77, Category.SOFTWARE, Priority.MEDIA);

        assertThat(new DeterministicTechnicianSelector(shuffled).select(77, Category.SOFTWARE, Priority.MEDIA))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "125, RED,      ALTA,    TEC-008, 2",
            "125, RED,      MEDIA,   TEC-013, 0",
            "125, RED,      CRITICA, TEC-008, 5",
            "7,   HARDWARE, CRITICA, TEC-003, 10",
            "1,   SOFTWARE, BAJA,    TEC-005, 9"})
    void knownInputsKeepTheirResultAcrossReleases(long ticketId, Category category, Priority priority,
            String expectedTechnician, int expectedHours) {
        var suggestion = selector.select(ticketId, category, priority);

        assertThat(suggestion.technicianId()).isEqualTo(expectedTechnician);
        assertThat(suggestion.estimatedHours()).isEqualTo(expectedHours);
    }

    @Test
    void theSuggestedTechnicianAlwaysCanHandleTheCategory() {
        for (long ticketId = 1; ticketId <= 500; ticketId++) {
            for (Category category : Category.values()) {
                for (Priority priority : Priority.values()) {
                    var suggestion = selector.select(ticketId, category, priority);

                    assertThat(byCode.get(suggestion.technicianId()).canHandle(category))
                            .as("%d %s %s -> %s", ticketId, category, priority, suggestion.technicianId())
                            .isTrue();
                    assertThat(suggestion.estimatedHours()).isBetween(0, 12);
                    assertThat(suggestion.availability())
                            .isEqualTo(Availability.fromEstimatedHours(suggestion.estimatedHours()));
                }
            }
        }
    }

    @Test
    void aRedTicketIsOnlySuggestedRedSpecialistsAndGeneralists() {
        Set<String> seen = new HashSet<>();
        for (long ticketId = 1; ticketId <= 300; ticketId++) {
            seen.add(selector.select(ticketId, Category.RED, Priority.MEDIA).technicianId());
        }

        assertThat(seen).containsExactlyInAnyOrder("TEC-007", "TEC-008", "TEC-009", "TEC-013", "TEC-014", "TEC-015");
    }

    @Test
    void aTicketOfCategoryOtroCanGoToAnyTechnician() {
        Set<String> seen = new HashSet<>();
        for (long ticketId = 1; ticketId <= 600; ticketId++) {
            seen.add(selector.select(ticketId, Category.OTRO, Priority.BAJA).technicianId());
        }

        assertThat(seen).hasSize(15);
    }

    @Test
    void differentCombinationsProduceDifferentEstimates() {
        Set<Integer> estimates = new HashSet<>();
        for (long ticketId = 1; ticketId <= 100; ticketId++) {
            estimates.add(selector.select(ticketId, Category.HARDWARE, Priority.MEDIA).estimatedHours());
        }

        assertThat(estimates.size()).isGreaterThan(5);
    }

    @Test
    void aCategoryWithoutAnyCompatibleTechnicianIsAnError() {
        var onlyHardware = new DeterministicTechnicianSelector(List.of(
                new DispatchTechnician("TEC-001", Category.HARDWARE)));

        assertThatThrownBy(() -> onlyHardware.select(1, Category.RED, Priority.ALTA))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("RED");
        assertThat(onlyHardware.select(1, Category.OTRO, Priority.ALTA).technicianId()).isEqualTo("TEC-001");
    }

    @Test
    void aGeneralistMakesEveryCategoryCoverable() {
        var withGeneralist = new DeterministicTechnicianSelector(List.of(
                new DispatchTechnician("TEC-001", Category.HARDWARE),
                new DispatchTechnician("TEC-002", Category.OTRO)));

        assertThat(withGeneralist.select(5, Category.ACCESOS, Priority.BAJA).technicianId()).isEqualTo("TEC-002");
    }

    @Test
    void rejectsAnInvalidPool() {
        assertThatThrownBy(() -> new DeterministicTechnicianSelector(List.of()))
                .isInstanceOf(DomainRuleViolationException.class);
        assertThatThrownBy(() -> new DeterministicTechnicianSelector(null))
                .isInstanceOf(DomainRuleViolationException.class);
        assertThatThrownBy(() -> new DeterministicTechnicianSelector(List.of(
                new DispatchTechnician("TEC-001", Category.RED), new DispatchTechnician("TEC-001", Category.RED))))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("TEC-001");
    }
}
