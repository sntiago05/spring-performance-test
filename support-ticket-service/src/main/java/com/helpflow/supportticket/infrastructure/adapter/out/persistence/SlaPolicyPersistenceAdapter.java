package com.helpflow.supportticket.infrastructure.adapter.out.persistence;

import com.helpflow.supportticket.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.SlaPolicy;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper.SlaPolicyEntityMapper;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository.SlaPolicyJpaRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class SlaPolicyPersistenceAdapter implements SlaPolicyRepositoryPort {

    private final SlaPolicyJpaRepository repository;
    private final SlaPolicyEntityMapper mapper;

    SlaPolicyPersistenceAdapter(SlaPolicyJpaRepository repository, SlaPolicyEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<SlaPolicy> findByPriority(Priority priority) {
        return repository.findById(priority).map(mapper::toDomain);
    }

    @Override
    public List<SlaPolicy> findAll() {
        return repository.findAll().stream().map(mapper::toDomain)
                .sorted(Comparator.comparing(SlaPolicy::priority)).toList();
    }

    @Override
    public SlaPolicy save(SlaPolicy policy) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(policy)));
    }
}
