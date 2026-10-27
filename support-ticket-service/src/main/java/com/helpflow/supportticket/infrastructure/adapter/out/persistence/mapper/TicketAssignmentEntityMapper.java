package com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.supportticket.domain.model.TicketAssignment;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketAssignmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TicketAssignmentEntityMapper {

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "technicianId", source = "technician.id")
    TicketAssignment toDomain(TicketAssignmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "technician", ignore = true)
    TicketAssignmentEntity toEntity(TicketAssignment assignment);
}
