package com.helpflow.supportticket.infrastructure.adapter.out.persistence;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.TicketSearchCriteria;
import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.port.out.TicketRepositoryPort;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TechnicianEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper.TicketEntityMapper;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository.TicketJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class TicketPersistenceAdapter implements TicketRepositoryPort {

    private static final Set<TicketStatus> SLA_RUNNING = Arrays.stream(TicketStatus.values())
            .filter(TicketStatus::isSlaRunning)
            .collect(Collectors.toSet());

    private final TicketJpaRepository repository;
    private final TicketEntityMapper mapper;
    private final EntityManager entityManager;

    TicketPersistenceAdapter(TicketJpaRepository repository, TicketEntityMapper mapper,
            EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity entity;
        if (ticket.id() == null) {
            entity = new TicketEntity();
            entity.setRequester(entityManager.getReference(UserEntity.class, ticket.requesterId()));
        } else {
            entity = repository.findById(ticket.id()).orElseThrow();
        }
        mapper.update(ticket, entity);
        entity.setAssignedTechnician(ticket.assignedTechnicianId() == null ? null
                : entityManager.getReference(TechnicianEntity.class, ticket.assignedTechnicianId()));
        // Flush now so an optimistic-lock conflict surfaces inside the use case that caused it.
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TicketView> findViewById(Long id) {
        return repository.findDetailedById(id).map(mapper::toView);
    }

    @Override
    public PageResult<TicketView> search(TicketSearchCriteria criteria, PageQuery page) {
        Specification<TicketEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.requesterId() != null) {
                predicates.add(cb.equal(root.get("requester").get("id"), criteria.requesterId()));
            }
            if (criteria.technicianId() != null) {
                predicates.add(cb.equal(root.get("assignedTechnician").get("id"), criteria.technicianId()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), criteria.priority()));
            }
            if (criteria.category() != null) {
                predicates.add(cb.equal(root.get("category"), criteria.category()));
            }
            if (criteria.overdueAt() != null) {
                predicates.add(root.get("status").in(SLA_RUNNING));
                predicates.add(cb.lessThan(root.<Instant>get("slaDueAt"), criteria.overdueAt()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return JpaPaging.toResult(repository.findAll(spec, JpaPaging.toPageable(page, "createdAt"))
                .map(mapper::toView));
    }

    @Override
    public List<Long> findIdsPendingSlaReview() {
        return repository.findIdsPendingSlaReview(SLA_RUNNING);
    }

    @Override
    public long countOverdue(Instant now) {
        return repository.countOverdue(SLA_RUNNING, now);
    }
}
