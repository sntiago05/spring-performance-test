package com.helpflow.supportticket.infrastructure.adapter.out.persistence;

import com.helpflow.supportticket.application.port.out.TicketHistoryPort;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketHistoryEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper.TicketHistoryEntityMapper;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository.TicketHistoryJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class TicketHistoryPersistenceAdapter implements TicketHistoryPort {

    private final TicketHistoryJpaRepository repository;
    private final TicketHistoryEntityMapper mapper;
    private final EntityManager entityManager;

    TicketHistoryPersistenceAdapter(TicketHistoryJpaRepository repository, TicketHistoryEntityMapper mapper,
            EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public TicketHistoryEntry append(TicketHistoryEntry entry) {
        TicketHistoryEntity entity = mapper.toEntity(entry);
        entity.setTicket(entityManager.getReference(TicketEntity.class, entry.ticketId()));
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<TicketHistoryEntry> findByTicketId(Long ticketId) {
        return repository.findByTicketIdOrderByOccurredAtAscIdAsc(ticketId).stream().map(mapper::toDomain).toList();
    }
}
