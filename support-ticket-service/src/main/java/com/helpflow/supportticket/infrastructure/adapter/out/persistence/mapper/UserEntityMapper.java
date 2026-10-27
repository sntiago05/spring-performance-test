package com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.supportticket.application.model.UserSummary;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface UserEntityMapper {

    // MapStruct reads the one-argument domain behaviors (withX, assignTo...) as fluent setters: ignore them.
    @Mapping(target = "withStatus", ignore = true)
    User toDomain(UserEntity entity);

    UserSummary toSummary(UserEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tickets", ignore = true)
    void update(User user, @MappingTarget UserEntity entity);
}
