package com.helpflow.supportticket.infrastructure.adapter.out.persistence.mapper;

import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TicketHistoryEntityMapper {

    @Mapping(target = "ticketId", source = "ticket.id")
    TicketHistoryEntry toDomain(TicketHistoryEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    TicketHistoryEntity toEntity(TicketHistoryEntry entry);
}
