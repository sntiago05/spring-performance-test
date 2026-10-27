package com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = {UserEntityMapper.class, TechnicianEntityMapper.class})
public interface TicketEntityMapper {

    // MapStruct reads the one-argument domain behaviors (withX, assignTo...) as fluent setters: ignore them.
    @Mapping(target = "assignTo", ignore = true)
    @Mapping(target = "escalate", ignore = true)
    @Mapping(target = "requesterId", source = "requester.id")
    @Mapping(target = "assignedTechnicianId", source = "assignedTechnician.id")
    Ticket toDomain(TicketEntity entity);

    @Mapping(target = "ticket", source = ".")
    @Mapping(target = "requester", source = "requester")
    @Mapping(target = "technician", source = "assignedTechnician")
    TicketView toView(TicketEntity entity);

    /**
     * Copies the scalar state. Associations (requester, technician) and the optimistic-lock version are
     * managed by the persistence adapter / Hibernate.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "requester", ignore = true)
    @Mapping(target = "assignedTechnician", ignore = true)
    @Mapping(target = "history", ignore = true)
    void update(Ticket ticket, @MappingTarget TicketEntity entity);
}
