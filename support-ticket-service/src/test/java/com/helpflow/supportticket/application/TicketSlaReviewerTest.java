package com.helpflow.supportticket.application.service;

import static com.helpflow.supportticket.support.Fixtures.NOW;
import static com.helpflow.supportticket.support.Fixtures.openTicket;
import static com.helpflow.supportticket.support.Fixtures.sla;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpflow.supportticket.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import com.helpflow.supportticket.domain.model.TicketStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketSlaReviewerTest {

    @Mock TicketRepositoryPort tickets;
    @Mock TicketHistoryPort history;
    @Mock SlaPolicyRepositoryPort slaPolicies;
    @Mock TicketMetricsPort metrics;

    // 8 hour window starting at NOW, escalation at 80% = 6h24m
    private final Ticket ticket = openTicket(5, Category.RED, Priority.MEDIA, "BOGOTA").assignTo(7L);

    private TicketSlaReviewer reviewerAt(Duration elapsed) {
        Instant clockNow = NOW.plus(elapsed);
        return new TicketSlaReviewer(tickets, history, slaPolicies, metrics, Clock.fixed(clockNow, ZoneOffset.UTC));
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(tickets.findById(5L)).thenReturn(Optional.of(ticket));
        org.mockito.Mockito.lenient().when(slaPolicies.findByPriority(Priority.MEDIA))
                .thenReturn(Optional.of(sla(Priority.MEDIA, 8)));
        org.mockito.Mockito.lenient().when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void doesNothingBeforeTheThreshold() {
        var outcome = reviewerAt(Duration.ofHours(5)).review(5L);

        assertThat(outcome).isEqualTo(new TicketSlaReviewer.Outcome(false, false));
        verify(tickets, never()).save(any());
        verify(history, never()).append(any());
    }

    @Test
    void escalatesTheTicketCloseToItsDeadline() {
        var outcome = reviewerAt(Duration.ofHours(7)).review(5L);

        assertThat(outcome).isEqualTo(new TicketSlaReviewer.Outcome(true, false));
        ArgumentCaptor<Ticket> saved = ArgumentCaptor.forClass(Ticket.class);
        verify(tickets).save(saved.capture());
        assertThat(saved.getValue().priority()).isEqualTo(Priority.ALTA);
        assertThat(saved.getValue().escalated()).isTrue();
        ArgumentCaptor<TicketHistoryEntry> entry = ArgumentCaptor.forClass(TicketHistoryEntry.class);
        verify(history).append(entry.capture());
        assertThat(entry.getValue().action()).isEqualTo(HistoryAction.ESCALATED);
        assertThat(entry.getValue().performedBy()).isEqualTo(TicketHistoryEntry.SYSTEM);
        assertThat(entry.getValue().observations()).contains("80%", "MEDIA -> ALTA");
        verify(metrics).ticketEscalated();
        verify(metrics, never()).slaBreached();
    }

    @Test
    void aTicketDiscoveredAfterItsDeadlineIsBothEscalatedAndFlaggedAsBreached() {
        var outcome = reviewerAt(Duration.ofHours(9)).review(5L);

        assertThat(outcome).isEqualTo(new TicketSlaReviewer.Outcome(true, true));
        ArgumentCaptor<TicketHistoryEntry> entries = ArgumentCaptor.forClass(TicketHistoryEntry.class);
        verify(history, times(2)).append(entries.capture());
        assertThat(entries.getAllValues()).extracting(TicketHistoryEntry::action)
                .containsExactly(HistoryAction.ESCALATED, HistoryAction.SLA_BREACHED);
        verify(metrics).ticketEscalated();
        verify(metrics).slaBreached();
    }

    @Test
    void anAlreadyEscalatedTicketOnlyGetsTheBreachFlagOnce() {
        Ticket escalated = ticket.escalate(NOW.plus(Duration.ofHours(7)));
        when(tickets.findById(5L)).thenReturn(Optional.of(escalated));
        when(slaPolicies.findByPriority(Priority.ALTA)).thenReturn(Optional.of(sla(Priority.ALTA, 8)));

        var outcome = reviewerAt(Duration.ofHours(9)).review(5L);

        assertThat(outcome).isEqualTo(new TicketSlaReviewer.Outcome(false, true));
        verify(metrics, never()).ticketEscalated();
    }

    @Test
    void ignoresTicketsThatAreNoLongerRunningAgainstTheSla() {
        Ticket resolved = ticket.transitionTo(TicketStatus.EN_PROGRESO, NOW).transitionTo(TicketStatus.RESUELTO, NOW);
        when(tickets.findById(5L)).thenReturn(Optional.of(resolved));

        var outcome = reviewerAt(Duration.ofHours(20)).review(5L);

        assertThat(outcome).isEqualTo(new TicketSlaReviewer.Outcome(false, false));
        verify(tickets, never()).save(any());
    }
}
