package com.helpflow.supportticket.infrastructure.adapter.out.persistence;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.domain.model.UserStatus;
import com.helpflow.supportticket.domain.service.TicketAssignmentPolicy;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TechnicianEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper.TechnicianEntityMapper;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository.TechnicianJpaRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class TechnicianPersistenceAdapter implements TechnicianRepositoryPort {

    private static final Set<TicketStatus> WORKLOAD = Arrays.stream(TicketStatus.values())
            .filter(TicketStatus::isTechnicianWorkload)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(TicketStatus.class)));

    private final TechnicianJpaRepository repository;
    private final TechnicianEntityMapper mapper;

    TechnicianPersistenceAdapter(TechnicianJpaRepository repository, TechnicianEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Technician save(Technician technician) {
        TechnicianEntity entity = technician.id() == null ? new TechnicianEntity()
                : repository.findById(technician.id()).orElseThrow();
        mapper.update(technician, entity);
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Technician> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Technician> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<Technician> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<TicketAssignmentPolicy.Candidate> findAllWithWorkload() {
        return repository.findAllWithWorkload(WORKLOAD).stream()
                .map(load -> new TicketAssignmentPolicy.Candidate(mapper.toDomain(load.technician()),
                        (int) load.activeTickets()))
                .toList();
    }

    @Override
    public int countActiveTickets(Long technicianId) {
        return repository.countWorkload(technicianId, WORKLOAD);
    }

    @Override
    public PageResult<Technician> search(UserStatus status, Category specialty, PageQuery page) {
        Specification<TechnicianEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (specialty != null) {
                predicates.add(cb.equal(root.get("specialty"), specialty));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return JpaPaging.toResult(
                repository.findAll(spec, JpaPaging.toPageable(page, "code")).map(mapper::toDomain));
    }

    @Override
    public String nextCode() {
        return "TEC-%03d".formatted(repository.nextCodeNumber());
    }

    @Override
    public List<Technician> findWithoutUser() {
        return repository.findByUserIdIsNull().stream().map(mapper::toDomain).toList();
    }
}
