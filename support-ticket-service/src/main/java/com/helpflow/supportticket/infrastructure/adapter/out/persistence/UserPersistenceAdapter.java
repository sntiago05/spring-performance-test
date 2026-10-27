package com.helpflow.supportticket.infrastructure.adapter.out.persistence;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.UserSearchCriteria;
import com.helpflow.supportticket.application.port.out.UserRepositoryPort;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper.UserEntityMapper;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;
    private final UserEntityMapper mapper;

    UserPersistenceAdapter(UserJpaRepository repository, UserEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserEntity entity = user.id() == null ? new UserEntity() : repository.findById(user.id()).orElseThrow();
        mapper.update(user, entity);
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByDocument(String document) {
        return repository.existsByDocument(document);
    }

    @Override
    public PageResult<User> search(UserSearchCriteria criteria, PageQuery page) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.role() != null) {
                predicates.add(cb.equal(root.get("role"), criteria.role()));
            }
            if (criteria.site() != null && !criteria.site().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("site")), criteria.site().trim().toLowerCase()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return JpaPaging.toResult(repository.findAll(spec, JpaPaging.toPageable(page, "id")).map(mapper::toDomain));
    }
}
