package com.helpflow.supportticket.application.port.out;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.UserStatus;
import com.helpflow.supportticket.domain.service.TicketAssignmentPolicy;
import java.util.List;
import java.util.Optional;

public interface TechnicianRepositoryPort {

    Technician save(Technician technician);

    Optional<Technician> findById(Long id);

    Optional<Technician> findByUserId(Long userId);

    /**
     * Loads the technician taking a row lock until the surrounding transaction ends, so that two
     * concurrent assignments cannot both fill the last free slot.
     */
    Optional<Technician> findByIdForUpdate(Long id);

    /**
     * Every technician with the number of tickets currently in their workload (single query).
     */
    List<TicketAssignmentPolicy.Candidate> findAllWithWorkload();

    int countActiveTickets(Long technicianId);

    PageResult<Technician> search(UserStatus status, Category specialty, PageQuery page);

    /**
     * Next technician code ({@code TEC-016}, ...) taken from a database sequence.
     */
    String nextCode();

    /**
     * Technicians that have no login account yet (seed data).
     */
    List<Technician> findWithoutUser();
}
