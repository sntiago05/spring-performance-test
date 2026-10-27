package com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.supportticket.domain.model.SlaPolicy;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.SlaPolicyEntity;
import org.mapstruct.Mapper;

@Mapper
public interface SlaPolicyEntityMapper {

    SlaPolicy toDomain(SlaPolicyEntity entity);

    SlaPolicyEntity toEntity(SlaPolicy policy);
}
