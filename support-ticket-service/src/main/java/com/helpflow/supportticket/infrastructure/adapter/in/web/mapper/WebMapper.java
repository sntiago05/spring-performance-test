package com.helpflow.supportticket.infrastructure.adapter.in.web.mapper;

import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.TechnicianSummary;
import com.helpflow.supportticket.application.model.TicketView;
import com.helpflow.supportticket.application.model.UserSummary;
import com.helpflow.supportticket.domain.model.SlaPolicy;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.TicketAssignment;
import com.helpflow.supportticket.domain.model.TicketHistoryEntry;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.AssignmentResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.HistoryResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.PageResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.SlaPolicyResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.TechnicianResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.TicketResponse;
import com.helpflow.supportticket.infrastructure.adapter.in.web.dto.UserResponse;
import java.util.function.Function;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Domain/read models to API DTOs. Passwords never appear in any response type.
 */
@Mapper
public interface WebMapper {

    UserResponse toResponse(User user);

    TechnicianResponse toResponse(Technician technician);

    @Mapping(target = ".", source = "ticket")
    @Mapping(target = "requester", source = "requester")
    @Mapping(target = "technician", source = "technician")
    TicketResponse toResponse(TicketView view);

    TicketResponse.UserRef toRef(UserSummary summary);

    TicketResponse.TechnicianRef toRef(TechnicianSummary summary);

    HistoryResponse toResponse(TicketHistoryEntry entry);

    AssignmentResponse toResponse(TicketAssignment assignment);

    SlaPolicyResponse toResponse(SlaPolicy policy);

    default <T, R> PageResponse<R> toPage(PageResult<T> page, Function<T, R> mapper) {
        PageResult<R> mapped = page.map(mapper);
        return new PageResponse<>(mapped.content(), mapped.page(), mapped.size(), mapped.totalElements(),
                mapped.totalPages());
    }
}
