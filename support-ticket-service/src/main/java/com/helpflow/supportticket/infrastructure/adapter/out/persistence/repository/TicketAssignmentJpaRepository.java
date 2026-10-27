package com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketAssignmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketAssignmentJpaRepository extends JpaRepository<TicketAssignmentEntity, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("update TicketAssignmentEntity a set a.active = false where a.ticket.id = :ticketId and a.active = true")
    void deactivateActiveOf(@Param("ticketId") Long ticketId);

    List<TicketAssignmentEntity> findByTicketIdOrderByAssignedAtAscIdAsc(Long ticketId);
}
