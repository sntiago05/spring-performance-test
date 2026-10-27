package com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.supportticket.application.model.TechnicianSummary;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TechnicianEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface TechnicianEntityMapper {

    // MapStruct reads the one-argument domain behaviors (withX, assignTo...) as fluent setters: ignore them.
    @Mapping(target = "withUser", ignore = true)
    Technician toDomain(TechnicianEntity entity);

    TechnicianSummary toSummary(TechnicianEntity entity);

    @Mapping(target = "id", ignore = true)
    void update(Technician technician, @MappingTarget TechnicianEntity entity);
}
