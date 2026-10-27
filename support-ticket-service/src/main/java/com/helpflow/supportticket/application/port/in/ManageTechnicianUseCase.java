package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.UserStatus;

public interface ManageTechnicianUseCase {

    /**
     * Creates the login account (ROLE_TECNICO) and the support profile in one transaction.
     */
    Technician create(CreateCommand command);

    Technician get(Long technicianId);

    PageResult<Technician> list(UserStatus status, Category specialty, PageQuery page);

    Technician update(Long technicianId, UpdateCommand command);

    record CreateCommand(String document, String fullName, String email, String rawPassword, String site,
            String area, Category specialty, int maxActiveTickets) {
    }

    record UpdateCommand(String fullName, Category specialty, String site, int maxActiveTickets,
            UserStatus status) {
    }
}
