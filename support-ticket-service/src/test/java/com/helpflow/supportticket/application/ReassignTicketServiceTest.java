package com.helpflow.supportticket.application.service;

import static com.helpflow.supportticket.support.Fixtures.NOW;
import static com.helpflow.supportticket.support.Fixtures.actor;
import static com.helpflow.supportticket.support.Fixtures.inactive;
import static com.helpflow.supportticket.support.Fixtures.openTicket;
import static com.helpflow.supportticket.support.Fixtures.technician;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.application.port.out.TicketAssignmentPort;
import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.ForbiddenOperationException;
import com.helpflow.supportticket.domain.model.AssignmentSource;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.TicketAssignment;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import com.helpflow.supportticket.domain.model.TicketStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReassignTicketServiceTest {

    @Mock TicketRepositoryPort tickets;
    @Mock TechnicianRepositoryPort technicians;
    @Mock TicketAssignmentPort assignments;
    @Mock TicketHistoryPort history;
    @Mock TicketMetricsPort metrics;

    private ReassignTicketService service;
    private final Ticket assigned = openTicket(5, Category.RED, Priority.ALTA, "BOGOTA").assignTo(7L);

    @BeforeEach
    void setUp() {
        TicketAssigner assigner = new TicketAssigner(tickets, assignments, history, metrics,
                Clock.fixed(NOW, ZoneOffset.UTC));
        service = new ReassignTicketService(tickets, technicians, assigner);
        org.mockito.Mockito.lenient().when(tickets.findById(5L)).thenReturn(Optional.of(assigned));
    }

    private void target(Technician technician, int active) {
        when(technicians.findByIdForUpdate(technician.id())).thenReturn(Optional.of(technician));
        org.mockito.Mockito.lenient().when(technicians.countActiveTickets(technician.id())).thenReturn(active);
    }

    @Test
    void anAdminMovesTheTicketToACompatibleTechnicianAndTheHistoryRecordsIt() {
        Technician target = technician(9, Category.RED, "BOGOTA", 3);
        target(target, 1);
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tickets.findViewById(5L)).thenReturn(Optional.of(new TicketView(null, null, null)));

        service.reassign(actor(1, Role.ROLE_ADMIN), 5L, 9L, "Closer to the user");

        ArgumentCaptor<Ticket> saved = ArgumentCaptor.forClass(Ticket.class);
        verify(tickets).save(saved.capture());
        assertThat(saved.getValue().assignedTechnicianId()).isEqualTo(9L);
        assertThat(saved.getValue().status()).isEqualTo(TicketStatus.ASIGNADO);

        verify(assignments).deactivateActiveOf(5L);
        ArgumentCaptor<TicketAssignment> assignment = ArgumentCaptor.forClass(TicketAssignment.class);
        verify(assignments).save(assignment.capture());
        assertThat(assignment.getValue().source()).isEqualTo(AssignmentSource.MANUAL);

        ArgumentCaptor<TicketHistoryEntry> entry = ArgumentCaptor.forClass(TicketHistoryEntry.class);
        verify(history).append(entry.capture());
        assertThat(entry.getValue().action()).isEqualTo(HistoryAction.REASSIGNED);
        assertThat(entry.getValue().observations()).contains("TEC-009", "Closer to the user");
    }

    @Test
    void onlyAdminsCanAssign() {
        assertThatThrownBy(() -> service.reassign(actor(2, Role.ROLE_TECNICO), 5L, 9L, "x"))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(tickets, never()).save(any());
    }

    @Test
    void rejectsAnInactiveTechnician() {
        target(inactive(technician(9, Category.RED, "BOGOTA", 3)), 0);

        assertThatThrownBy(() -> service.reassign(actor(1, Role.ROLE_ADMIN), 5L, 9L, "x"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("technician-inactive");
    }

    @Test
    void rejectsATechnicianWhoseSpecialtyDoesNotMatch() {
        target(technician(9, Category.SOFTWARE, "BOGOTA", 3), 0);

        assertThatThrownBy(() -> service.reassign(actor(1, Role.ROLE_ADMIN), 5L, 9L, "x"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("technician-incompatible");
    }

    @Test
    void rejectsATechnicianAtCapacity() {
        target(technician(9, Category.RED, "BOGOTA", 2), 2);

        assertThatThrownBy(() -> service.reassign(actor(1, Role.ROLE_ADMIN), 5L, 9L, "x"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("technician-at-capacity");
    }

    @Test
    void rejectsReassigningToTheCurrentOwner() {
        target(technician(7, Category.RED, "BOGOTA", 3), 1);

        assertThatThrownBy(() -> service.reassign(actor(1, Role.ROLE_ADMIN), 5L, 7L, "x"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code").isEqualTo("already-assigned");
    }
}
