package com.helpflow.supportticket.infrastructure.adapter.out.persistence;

import com.helpflow.supportticket.application.port.out.TicketAssignmentPort;
import com.helpflow.supportticket.domain.model.TicketAssignment;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TechnicianEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketAssignmentEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper.TicketAssignmentEntityMapper;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository.TicketAssignmentJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class TicketAssignmentPersistenceAdapter implements TicketAssignmentPort {

    private final TicketAssignmentJpaRepository repository;
    private final TicketAssignmentEntityMapper mapper;
    private final EntityManager entityManager;

    TicketAssignmentPersistenceAdapter(TicketAssignmentJpaRepository repository,
            TicketAssignmentEntityMapper mapper, EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public TicketAssignment save(TicketAssignment assignment) {
        TicketAssignmentEntity entity = mapper.toEntity(assignment);
        entity.setTicket(entityManager.getReference(TicketEntity.class, assignment.ticketId()));
        entity.setTechnician(entityManager.getReference(TechnicianEntity.class, assignment.technicianId()));
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public void deactivateActiveOf(Long ticketId) {
        repository.deactivateActiveOf(ticketId);
    }

    @Override
    public List<TicketAssignment> findByTicketId(Long ticketId) {
        return repository.findByTicketIdOrderByAssignedAtAscIdAsc(ticketId).stream().map(mapper::toDomain).toList();
    }
}
