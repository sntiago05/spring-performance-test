package com.helpflow.supportticket.application.service;

import static com.helpflow.supportticket.support.Fixtures.NOW;
import static com.helpflow.supportticket.support.Fixtures.actor;
import static com.helpflow.supportticket.support.Fixtures.technician;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.TicketSearchCriteria;
import com.helpflow.supportticket.application.port.in.SearchTicketsUseCase;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.exception.ForbiddenOperationException;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Role;
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

/**
 * Visibility rules of the ticket listing: the role decides what a caller can ever see, whatever filters
 * they send.
 */
@ExtendWith(MockitoExtension.class)
class SearchTicketsServiceTest {

    @Mock TicketRepositoryPort tickets;
    @Mock TechnicianRepositoryPort technicians;

    private SearchTicketsService service;
    private final PageQuery page = PageQuery.of(0, 20);
    private final SearchTicketsUseCase.Filter spoofedFilter = new SearchTicketsUseCase.Filter(999L, 888L,
            TicketStatus.ASIGNADO, Priority.ALTA, Category.RED, false);

    @BeforeEach
    void setUp() {
        service = new SearchTicketsService(tickets, new TicketAccess(technicians), Clock.fixed(NOW, ZoneOffset.UTC));
        org.mockito.Mockito.lenient().when(tickets.search(any(), any())).thenReturn(PageResult.empty(page));
    }

    private TicketSearchCriteria capturedCriteria() {
        ArgumentCaptor<TicketSearchCriteria> captor = ArgumentCaptor.forClass(TicketSearchCriteria.class);
        verify(tickets).search(captor.capture(), any());
        return captor.getValue();
    }

    @Test
    void usersAlwaysSeeOnlyTheirOwnTickets() {
        service.search(actor(10, Role.ROLE_USUARIO), spoofedFilter, page);

        TicketSearchCriteria criteria = capturedCriteria();
        assertThat(criteria.requesterId()).isEqualTo(10L);
        assertThat(criteria.technicianId()).isNull();
        assertThat(criteria.status()).isEqualTo(TicketStatus.ASIGNADO); // other filters still apply
    }

    @Test
    void techniciansAlwaysSeeOnlyTheirAssignedTickets() {
        when(technicians.findByUserId(20L)).thenReturn(Optional.of(technician(7, Category.RED, "BOGOTA", 3)));

        service.search(actor(20, Role.ROLE_TECNICO), spoofedFilter, page);

        TicketSearchCriteria criteria = capturedCriteria();
        assertThat(criteria.technicianId()).isEqualTo(7L);
        assertThat(criteria.requesterId()).isNull();
    }

    @Test
    void techniciansWithoutAProfileAreRejected() {
        when(technicians.findByUserId(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.search(actor(20, Role.ROLE_TECNICO), spoofedFilter, page))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void adminsKeepTheirFiltersAndCanAskForOverdueTickets() {
        var filter = new SearchTicketsUseCase.Filter(999L, 888L, null, null, null, true);

        service.search(actor(1, Role.ROLE_ADMIN), filter, page);

        TicketSearchCriteria criteria = capturedCriteria();
        assertThat(criteria.requesterId()).isEqualTo(999L);
        assertThat(criteria.technicianId()).isEqualTo(888L);
        assertThat(criteria.overdueAt()).isEqualTo(NOW);
    }
}
