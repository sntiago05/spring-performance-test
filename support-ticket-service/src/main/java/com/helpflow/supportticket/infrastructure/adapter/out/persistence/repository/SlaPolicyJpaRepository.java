package com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.SlaPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaPolicyJpaRepository extends JpaRepository<SlaPolicyEntity, Priority> {
}
