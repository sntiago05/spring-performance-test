package com.helpflow.supportticket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpflow.supportticket.domain.model.TicketStatus;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.helpflow.supportticket.domain.model.TicketStatus.*;

class TicketStatusTest {

    static Stream<Arguments> allowed() {
        return Stream.of(
                Arguments.of(ABIERTO, ASIGNADO), Arguments.of(ABIERTO, CANCELADO),
                Arguments.of(ASIGNADO, EN_PROGRESO), Arguments.of(ASIGNADO, CANCELADO),
                Arguments.of(EN_PROGRESO, ASIGNADO), Arguments.of(EN_PROGRESO, RESUELTO),
                Arguments.of(EN_PROGRESO, CANCELADO), Arguments.of(RESUELTO, EN_PROGRESO),
                Arguments.of(RESUELTO, CERRADO));
    }

    @ParameterizedTest
    @MethodSource("allowed")
    void allowsTheDocumentedTransitions(TicketStatus from, TicketStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @Test
    void everythingElseIsRejected() {
        Set<String> allowedPairs = allowed()
                .map(args -> args.get()[0] + ">" + args.get()[1])
                .collect(java.util.stream.Collectors.toSet());

        for (TicketStatus from : values()) {
            for (TicketStatus to : values()) {
                assertThat(from.canTransitionTo(to))
                        .as("%s -> %s", from, to)
                        .isEqualTo(allowedPairs.contains(from + ">" + to));
            }
        }
    }

    @Test
    void finalStatusesHaveNoExit() {
        assertThat(CERRADO.allowedTargets()).isEmpty();
        assertThat(CANCELADO.allowedTargets()).isEmpty();
    }

    @Test
    void slaRunsOnlyWhileTheTicketIsUnresolved() {
        assertThat(EnumSet.allOf(TicketStatus.class).stream().filter(TicketStatus::isSlaRunning))
                .containsExactlyInAnyOrder(ABIERTO, ASIGNADO, EN_PROGRESO);
    }

    @Test
    void workloadCountsAssignedAndInProgressTickets() {
        assertThat(EnumSet.allOf(TicketStatus.class).stream().filter(TicketStatus::isTechnicianWorkload))
                .containsExactlyInAnyOrder(ASIGNADO, EN_PROGRESO);
    }
}
