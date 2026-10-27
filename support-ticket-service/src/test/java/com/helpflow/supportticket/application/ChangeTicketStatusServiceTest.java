package com.helpflow.supportticket.application.service;

import static com.helpflow.supportticket.support.Fixtures.NOW;
import static com.helpflow.supportticket.support.Fixtures.actor;
import static com.helpflow.supportticket.support.Fixtures.openTicket;
import static com.helpflow.supportticket.support.Fixtures.technician;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.exception.ForbiddenOperationException;
import com.helpflow.supportticket.domain.exception.InvalidTicketTransitionException;
import com.helpflow.supportticket.domain.exception.ResourceNotFoundException;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Role;
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
class ChangeTicketStatusServiceTest {

    @Mock TicketRepositoryPort tickets;
    @Mock TicketHistoryPort history;
    @Mock TicketMetricsPort metrics;
    @Mock TechnicianRepositoryPort technicians;

    private final Instant later = NOW.plus(Duration.ofHours(2));
    private ChangeTicketStatusService service;

    @BeforeEach
    void setUp() {
        service = new ChangeTicketStatusService(tickets, history, metrics, new TicketAccess(technicians),
                Clock.fixed(later, ZoneOffset.UTC));
    }

    private Ticket assignedToTechnician7() {
        return openTicket(5, Category.RED, Priority.ALTA, "BOGOTA").assignTo(7L);
    }

    private void technicianUserOwns(long technicianId) {
        when(technicians.findByUserId(50L)).thenReturn(Optional.of(technician(technicianId, Category.RED, "BOGOTA", 3)));
    }

    @Test
    void theAssignedTechnicianStartsWorkAndTheFirstResponseTimeIsMeasured() {
        when(tickets.findById(5L)).thenReturn(Optional.of(assignedToTechnician7()));
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tickets.findViewById(5L)).thenReturn(Optional.of(new TicketView(null, null, null)));
        technicianUserOwns(7);

        service.changeStatus(actor(50, Role.ROLE_TECNICO), 5L, TicketStatus.EN_PROGRESO, " On my way ");

        ArgumentCaptor<Ticket> saved = ArgumentCaptor.forClass(Ticket.class);
        verify(tickets).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(TicketStatus.EN_PROGRESO);
        assertThat(saved.getValue().firstResponseAt()).isEqualTo(later);

        ArgumentCaptor<TicketHistoryEntry> entry = ArgumentCaptor.forClass(TicketHistoryEntry.class);
        verify(history).append(entry.capture());
        assertThat(entry.getValue().action()).isEqualTo(HistoryAction.STATUS_CHANGED);
        assertThat(entry.getValue().fromStatus()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(entry.getValue().toStatus()).isEqualTo(TicketStatus.EN_PROGRESO);
        assertThat(entry.getValue().observations()).isEqualTo("On my way");
        verify(metrics).firstResponse(Duration.ofHours(2));
    }

    @Test
    void resolvingAfterTheDeadlineRecordsResolutionTimeAndTheSlaBreach() {
        Ticket inProgress = assignedToTechnician7().transitionTo(TicketStatus.EN_PROGRESO, NOW.plusSeconds(60));
        Instant afterDeadline = NOW.plus(Duration.ofHours(9));
        ChangeTicketStatusService lateService = new ChangeTicketStatusService(tickets, history, metrics,
                new TicketAccess(technicians), Clock.fixed(afterDeadline, ZoneOffset.UTC));
        when(tickets.findById(5L)).thenReturn(Optional.of(inProgress));
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tickets.findViewById(5L)).thenReturn(Optional.of(new TicketView(null, null, null)));
        technicianUserOwns(7);

        lateService.changeStatus(actor(50, Role.ROLE_TECNICO), 5L, TicketStatus.RESUELTO, "Replaced the access point");

        verify(metrics).resolution(Duration.ofHours(9));
        verify(metrics).slaBreached();
        verify(metrics, never()).firstResponse(any());
        ArgumentCaptor<TicketHistoryEntry> entries = ArgumentCaptor.forClass(TicketHistoryEntry.class);
        verify(history, times(2)).append(entries.capture());
        assertThat(entries.getAllValues()).extracting(TicketHistoryEntry::action)
                .containsExactly(HistoryAction.STATUS_CHANGED, HistoryAction.SLA_BREACHED);
    }

    @Test
    void aTechnicianCannotTouchTicketsAssignedToSomeoneElse() {
        when(tickets.findById(5L)).thenReturn(Optional.of(assignedToTechnician7()));
        technicianUserOwns(9);

        assertThatThrownBy(() -> service.changeStatus(actor(50, Role.ROLE_TECNICO), 5L, TicketStatus.EN_PROGRESO, "x"))
                .isInstanceOf(ForbiddenOperationException.class)
                .extracting("code").isEqualTo("ticket-manage-denied");
        verify(tickets, never()).save(any());
    }

    @Test
    void aTechnicianCannotCancel() {
        when(tickets.findById(5L)).thenReturn(Optional.of(assignedToTechnician7()));
        technicianUserOwns(7);

        assertThatThrownBy(() -> service.changeStatus(actor(50, Role.ROLE_TECNICO), 5L, TicketStatus.CANCELADO,
                "Cancelled because it is a duplicate"))
                .isInstanceOf(ForbiddenOperationException.class)
                .extracting("code").isEqualTo("cancel-admin-only");
    }

    @Test
    void anAdminCanCancelAnyOpenTicket() {
        when(tickets.findById(5L)).thenReturn(Optional.of(openTicket(5, Category.RED, Priority.ALTA, "BOGOTA")));
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tickets.findViewById(5L)).thenReturn(Optional.of(new TicketView(null, null, null)));

        service.changeStatus(actor(1, Role.ROLE_ADMIN), 5L, TicketStatus.CANCELADO, "Duplicate of ticket 4");

        ArgumentCaptor<Ticket> saved = ArgumentCaptor.forClass(Ticket.class);
        verify(tickets).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(TicketStatus.CANCELADO);
    }

    @Test
    void invalidTransitionsAreRejectedAndNothingIsStored() {
        when(tickets.findById(5L)).thenReturn(Optional.of(assignedToTechnician7()));
        technicianUserOwns(7);

        assertThatThrownBy(() -> service.changeStatus(actor(50, Role.ROLE_TECNICO), 5L, TicketStatus.CERRADO, "x"))
                .isInstanceOf(InvalidTicketTransitionException.class);
        verify(tickets, never()).save(any());
        verify(history, never()).append(any());
    }

    @Test
    void unknownTicketsAreReportedAsNotFound() {
        when(tickets.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(actor(1, Role.ROLE_ADMIN), 99L, TicketStatus.CANCELADO, "x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
