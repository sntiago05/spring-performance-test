package com.helpflow.supportticket.domain;

import static com.helpflow.supportticket.support.Fixtures.NOW;
import static com.helpflow.supportticket.support.Fixtures.openTicket;
import static com.helpflow.supportticket.support.Fixtures.sla;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.InvalidTicketTransitionException;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.TicketStatus;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketTest {

    @Test
    void newTicketIsOpenWithTheSlaDeadlineOfItsPriority() {
        Ticket ticket = Ticket.open(1L, "Title", "Description", Category.RED, Priority.ALTA, "BOGOTA", "AP-1", NOW,
                sla(Priority.ALTA, 8));

        assertThat(ticket.status()).isEqualTo(TicketStatus.ABIERTO);
        assertThat(ticket.slaDueAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
        assertThat(ticket.assignedTechnicianId()).isNull();
        assertThat(ticket.escalated()).isFalse();
        assertThat(ticket.slaBreached()).isFalse();
    }

    @Test
    void deadlineMustBeAfterCreation() {
        assertThatThrownBy(() -> new Ticket(null, 1L, "t", "d", Category.RED, Priority.ALTA, "S", "A",
                TicketStatus.ABIERTO, NOW, NOW, null, null, null, false, null, false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assigningMovesAnOpenTicketToAsignado() {
        Ticket assigned = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA").assignTo(7L);

        assertThat(assigned.status()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(assigned.assignedTechnicianId()).isEqualTo(7L);
    }

    @Test
    void reassigningATicketInProgressSendsItBackToAsignado() {
        Ticket inProgress = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA").assignTo(7L)
                .transitionTo(TicketStatus.EN_PROGRESO, NOW);

        Ticket reassigned = inProgress.assignTo(9L);

        assertThat(reassigned.status()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(reassigned.assignedTechnicianId()).isEqualTo(9L);
        assertThat(reassigned.firstResponseAt()).isEqualTo(NOW); // history of the first response is kept
    }

    @ParameterizedTest
    @CsvSource({"RESUELTO", "CERRADO", "CANCELADO"})
    void finishedTicketsCannotBeAssigned(TicketStatus finished) {
        Ticket ticket = ticketIn(finished);

        assertThatThrownBy(() -> ticket.assignTo(3L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("ticket-not-assignable");
    }

    @Test
    void firstTransitionToInProgressRecordsTheFirstResponseOnlyOnce() {
        Ticket ticket = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA").assignTo(7L);

        Ticket started = ticket.transitionTo(TicketStatus.EN_PROGRESO, NOW.plusSeconds(600));
        Ticket restarted = started.transitionTo(TicketStatus.RESUELTO, NOW.plusSeconds(1200))
                .transitionTo(TicketStatus.EN_PROGRESO, NOW.plusSeconds(1800));

        assertThat(started.firstResponseAt()).isEqualTo(NOW.plusSeconds(600));
        assertThat(restarted.firstResponseAt()).isEqualTo(NOW.plusSeconds(600));
    }

    @Test
    void resolvingWithinTheDeadlineIsNotABreach() {
        Ticket resolved = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA").assignTo(7L)
                .transitionTo(TicketStatus.EN_PROGRESO, NOW.plusSeconds(60))
                .transitionTo(TicketStatus.RESUELTO, NOW.plus(Duration.ofHours(7)));

        assertThat(resolved.resolvedAt()).isEqualTo(NOW.plus(Duration.ofHours(7)));
        assertThat(resolved.slaBreached()).isFalse();
    }

    @Test
    void resolvingAfterTheDeadlineFlagsTheBreach() {
        Ticket resolved = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA").assignTo(7L)
                .transitionTo(TicketStatus.EN_PROGRESO, NOW.plusSeconds(60))
                .transitionTo(TicketStatus.RESUELTO, NOW.plus(Duration.ofHours(9)));

        assertThat(resolved.slaBreached()).isTrue();
    }

    @Test
    void reopeningClearsTheResolutionDate() {
        Ticket reopened = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA").assignTo(7L)
                .transitionTo(TicketStatus.EN_PROGRESO, NOW)
                .transitionTo(TicketStatus.RESUELTO, NOW.plusSeconds(60))
                .transitionTo(TicketStatus.EN_PROGRESO, NOW.plusSeconds(120));

        assertThat(reopened.resolvedAt()).isNull();
    }

    @Test
    void invalidTransitionsAreRejectedWithTheStatusesInvolved() {
        Ticket ticket = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA").assignTo(7L);

        assertThatThrownBy(() -> ticket.transitionTo(TicketStatus.RESUELTO, NOW))
                .isInstanceOf(InvalidTicketTransitionException.class)
                .hasMessageContaining("ASIGNADO -> RESUELTO");
    }

    @Test
    void asignadoCanOnlyBeReachedByAssigning() {
        Ticket open = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA");

        assertThatThrownBy(() -> open.transitionTo(TicketStatus.ASIGNADO, NOW))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("assignment-required");
    }

    @Test
    void escalationRaisesThePriorityOnceAndKeepsTheDeadline() {
        Ticket ticket = openTicket(1, Category.RED, Priority.MEDIA, "BOGOTA");

        Ticket escalated = ticket.escalate(NOW.plusSeconds(3600));

        assertThat(escalated.priority()).isEqualTo(Priority.ALTA);
        assertThat(escalated.escalated()).isTrue();
        assertThat(escalated.escalatedAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(escalated.slaDueAt()).isEqualTo(ticket.slaDueAt());
    }

    @Test
    void criticalPriorityCannotGoHigher() {
        assertThat(Priority.CRITICA.escalate()).isEqualTo(Priority.CRITICA);
        assertThat(Priority.BAJA.escalate()).isEqualTo(Priority.MEDIA);
    }

    @ParameterizedTest
    @CsvSource({"3,false", "6,false", "7,true", "9,true"})
    void needsEscalationWhenTheThresholdOfTheWindowIsConsumed(long hoursElapsed, boolean expected) {
        // 8 hour window, 80% threshold -> 6.4 hours
        Ticket ticket = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA");

        assertThat(ticket.needsEscalation(NOW.plus(Duration.ofHours(hoursElapsed)), 80)).isEqualTo(expected);
    }

    @Test
    void anEscalatedOrFinishedTicketIsNeverEscalatedAgain() {
        Ticket escalated = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA").escalate(NOW);
        Ticket resolved = ticketIn(TicketStatus.RESUELTO);
        var late = NOW.plus(Duration.ofHours(20));

        assertThat(escalated.needsEscalation(late, 80)).isFalse();
        assertThat(resolved.needsEscalation(late, 80)).isFalse();
    }

    @Test
    void overdueMeansPastTheDeadlineWhileTheSlaIsRunning() {
        Ticket ticket = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA");

        assertThat(ticket.isOverdue(NOW.plus(Duration.ofHours(8)))).isFalse();
        assertThat(ticket.isOverdue(NOW.plus(Duration.ofHours(8)).plusSeconds(1))).isTrue();
        assertThat(ticketIn(TicketStatus.RESUELTO).isOverdue(NOW.plus(Duration.ofDays(5)))).isFalse();
    }

    private static Ticket ticketIn(TicketStatus status) {
        Ticket base = openTicket(1, Category.RED, Priority.ALTA, "BOGOTA");
        return new Ticket(base.id(), base.requesterId(), base.title(), base.description(), base.category(),
                base.priority(), base.site(), base.affectedAsset(), status, base.createdAt(), base.slaDueAt(), 7L,
                null, null, false, null, false, 0L);
    }
}
