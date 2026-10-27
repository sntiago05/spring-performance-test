package com.helpflow.supportticket.application.service;

import static com.helpflow.supportticket.support.Fixtures.NOW;
import static com.helpflow.supportticket.support.Fixtures.actor;
import static com.helpflow.supportticket.support.Fixtures.sla;
import static com.helpflow.supportticket.support.Fixtures.technician;
import static com.helpflow.supportticket.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.port.in.CreateTicketUseCase;
import com.helpflow.supportticket.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.supportticket.application.port.out.TechnicianDispatchPort;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.application.port.out.TicketAssignmentPort;
import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.application.port.out.UserRepositoryPort;
import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.TechnicianDispatchUnavailableException;
import com.helpflow.supportticket.domain.model.AssignmentSource;
import com.helpflow.supportticket.domain.model.Availability;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.DispatchSuggestion;
import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.TicketAssignment;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.domain.model.UserStatus;
import com.helpflow.supportticket.domain.service.TicketAssignmentPolicy.Candidate;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure use-case tests: every outbound port is a mock, including TechnicianDispatchPort.
 */
@ExtendWith(MockitoExtension.class)
class CreateTicketServiceTest {

    @Mock UserRepositoryPort users;
    @Mock TicketRepositoryPort tickets;
    @Mock TicketHistoryPort history;
    @Mock TicketAssignmentPort assignments;
    @Mock SlaPolicyRepositoryPort slaPolicies;
    @Mock TechnicianRepositoryPort technicians;
    @Mock TechnicianDispatchPort dispatch;
    @Mock TicketMetricsPort metrics;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private CreateTicketService service;

    private final CreateTicketUseCase.Command command = new CreateTicketUseCase.Command("Wi-Fi down",
            "The wireless network drops constantly", Category.RED, Priority.ALTA, "BOGOTA", "AP-3F-02");

    @BeforeEach
    void setUp() {
        TicketAssigner assigner = new TicketAssigner(tickets, assignments, history, metrics, clock);
        service = new CreateTicketService(users, tickets, history, slaPolicies, technicians, dispatch, assigner,
                metrics, clock);
        lenient().when(users.findById(1L)).thenReturn(Optional.of(user(1, UserStatus.ACTIVO, Role.ROLE_USUARIO)));
        lenient().when(slaPolicies.findByPriority(Priority.ALTA)).thenReturn(Optional.of(sla(Priority.ALTA, 8)));
        // persistence assigns ids and echoes what it is given
        lenient().when(tickets.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            return t.id() == null ? withId(t, 10L) : t;
        });
        lenient().when(tickets.findViewById(10L)).thenAnswer(inv -> Optional.of(new TicketView(null, null, null)));
    }

    private static Ticket withId(Ticket t, long id) {
        return new Ticket(id, t.requesterId(), t.title(), t.description(), t.category(), t.priority(), t.site(),
                t.affectedAsset(), t.status(), t.createdAt(), t.slaDueAt(), t.assignedTechnicianId(),
                t.firstResponseAt(), t.resolvedAt(), t.escalated(), t.escalatedAt(), t.slaBreached(), 0L);
    }

    private void stubLockedTechnician(Technician technician, int activeTickets) {
        when(technicians.findByIdForUpdate(technician.id())).thenReturn(Optional.of(technician));
        when(technicians.countActiveTickets(technician.id())).thenReturn(activeTickets);
    }

    private Ticket lastSavedTicket() {
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(tickets, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }

    @Test
    void registersTheTicketOpenWithItsSlaDeadlineAndAssignsTheSuggestedTechnician() {
        Technician suggested = technician(8, Category.RED, "MEDELLIN", 3);
        when(dispatch.suggest(10L, Category.RED, Priority.ALTA))
                .thenReturn(new DispatchSuggestion("TEC-008", Availability.MEDIA, 4));
        when(technicians.findAllWithWorkload()).thenReturn(List.of(new Candidate(suggested, 1)));
        stubLockedTechnician(suggested, 1);

        service.create(actor(1, Role.ROLE_USUARIO), command);

        Ticket saved = lastSavedTicket();
        assertThat(saved.status()).isEqualTo(TicketStatus.ASIGNADO);
        assertThat(saved.assignedTechnicianId()).isEqualTo(8L);
        assertThat(saved.slaDueAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));

        ArgumentCaptor<TicketAssignment> assignment = ArgumentCaptor.forClass(TicketAssignment.class);
        verify(assignments).save(assignment.capture());
        assertThat(assignment.getValue().source()).isEqualTo(AssignmentSource.DISPATCH);
        assertThat(assignment.getValue().estimatedHours()).isEqualTo(4);
        assertThat(assignment.getValue().withinSla()).isTrue();
        assertThat(assignment.getValue().active()).isTrue();
        verify(assignments).deactivateActiveOf(10L);

        ArgumentCaptor<TicketHistoryEntry> entries = ArgumentCaptor.forClass(TicketHistoryEntry.class);
        verify(history, org.mockito.Mockito.times(2)).append(entries.capture());
        assertThat(entries.getAllValues()).extracting(TicketHistoryEntry::action)
                .containsExactly(HistoryAction.CREATED, HistoryAction.ASSIGNED);
        verify(metrics).ticketCreated(Category.RED, Priority.ALTA);
        verify(metrics).ticketAssigned(AssignmentSource.DISPATCH);
    }

    @Test
    void fallsBackToTheInternalRulesWhenDispatchIsUnavailable() {
        Technician sameSite = technician(7, Category.RED, "BOGOTA", 3);
        Technician other = technician(9, Category.RED, "CALI", 3);
        when(dispatch.suggest(anyLong(), any(), any()))
                .thenThrow(new TechnicianDispatchUnavailableException("dispatch-unavailable", "timeout"));
        when(technicians.findAllWithWorkload()).thenReturn(List.of(new Candidate(other, 0), new Candidate(sameSite, 2)));
        stubLockedTechnician(sameSite, 2);

        service.create(actor(1, Role.ROLE_USUARIO), command);

        assertThat(lastSavedTicket().assignedTechnicianId()).isEqualTo(7L);
        ArgumentCaptor<TicketAssignment> assignment = ArgumentCaptor.forClass(TicketAssignment.class);
        verify(assignments).save(assignment.capture());
        assertThat(assignment.getValue().source()).isEqualTo(AssignmentSource.RULES);
        assertThat(assignment.getValue().estimatedHours()).isNull();
    }

    @Test
    void anIneligibleSuggestionIsRejectedAndTheHistoryExplainsIt() {
        Technician incompatible = technician(1, Category.SOFTWARE, "BOGOTA", 3);
        Technician network = technician(7, Category.RED, "BOGOTA", 3);
        when(dispatch.suggest(anyLong(), any(), any()))
                .thenReturn(new DispatchSuggestion("TEC-001", Availability.ALTA, 1));
        when(technicians.findAllWithWorkload()).thenReturn(List.of(new Candidate(incompatible, 0), new Candidate(network, 0)));
        stubLockedTechnician(network, 0);

        service.create(actor(1, Role.ROLE_USUARIO), command);

        assertThat(lastSavedTicket().assignedTechnicianId()).isEqualTo(7L);
        ArgumentCaptor<TicketHistoryEntry> entries = ArgumentCaptor.forClass(TicketHistoryEntry.class);
        verify(history, org.mockito.Mockito.times(2)).append(entries.capture());
        assertThat(entries.getAllValues().get(1).observations())
                .contains("TEC-007", "Dispatch suggestion TEC-001 rejected");
    }

    @Test
    void skipsATechnicianWhoseLastSlotWasTakenMeanwhile() {
        Technician first = technician(7, Category.RED, "BOGOTA", 2);
        Technician second = technician(9, Category.RED, "BOGOTA", 2);
        when(dispatch.suggest(anyLong(), any(), any()))
                .thenThrow(new TechnicianDispatchUnavailableException("dispatch-unavailable", "down"));
        when(technicians.findAllWithWorkload()).thenReturn(List.of(new Candidate(first, 1), new Candidate(second, 1)));
        // the row lock reveals that TEC-007 filled up after the candidate list was read
        stubLockedTechnician(first, 2);
        stubLockedTechnician(second, 1);

        service.create(actor(1, Role.ROLE_USUARIO), command);

        assertThat(lastSavedTicket().assignedTechnicianId()).isEqualTo(9L);
    }

    @Test
    void theTicketStaysOpenWhenNobodyIsEligible() {
        when(dispatch.suggest(anyLong(), any(), any()))
                .thenReturn(new DispatchSuggestion("TEC-001", Availability.ALTA, 1));
        when(technicians.findAllWithWorkload()).thenReturn(List.of());

        service.create(actor(1, Role.ROLE_USUARIO), command);

        assertThat(lastSavedTicket().status()).isEqualTo(TicketStatus.ABIERTO);
        verifyNoInteractions(assignments);
        ArgumentCaptor<TicketHistoryEntry> entries = ArgumentCaptor.forClass(TicketHistoryEntry.class);
        verify(history, org.mockito.Mockito.times(2)).append(entries.capture());
        assertThat(entries.getAllValues()).extracting(TicketHistoryEntry::action)
                .containsExactly(HistoryAction.CREATED, HistoryAction.ASSIGNMENT_PENDING);
        verify(metrics, never()).ticketAssigned(any());
    }

    @Test
    void inactiveUsersCannotRegisterTickets() {
        when(users.findById(2L)).thenReturn(Optional.of(user(2, UserStatus.INACTIVO, Role.ROLE_USUARIO)));

        assertThatThrownBy(() -> service.create(actor(2, Role.ROLE_USUARIO), command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("user-inactive");

        verify(tickets, never()).save(any());
        verifyNoInteractions(dispatch);
    }
}
