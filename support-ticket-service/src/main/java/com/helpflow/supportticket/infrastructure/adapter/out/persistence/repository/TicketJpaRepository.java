package com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, Long>, JpaSpecificationExecutor<TicketEntity> {

    /**
     * Single ticket with requester and technician joined: no lazy loading afterwards.
     */
    @EntityGraph(attributePaths = {"requester", "assignedTechnician"})
    @Query("select t from TicketEntity t where t.id = :id")
    Optional<TicketEntity> findDetailedById(@Param("id") Long id);

    /**
     * Paginated listing. The entity graph joins the many-to-one associations in the same select, so
     * a page of N tickets costs one query (plus the count) instead of 1 + 2N.
     */
    @Override
    @EntityGraph(attributePaths = {"requester", "assignedTechnician"})
    Page<TicketEntity> findAll(Specification<TicketEntity> spec, Pageable pageable);

    @Query("""
            select t.id from TicketEntity t
            where t.status in :runningStatuses and (t.escalated = false or t.slaBreached = false)
            order by t.id
            """)
    List<Long> findIdsPendingSlaReview(@Param("runningStatuses") Collection<TicketStatus> runningStatuses);

    @Query("select count(t) from TicketEntity t where t.status in :runningStatuses and t.slaDueAt < :now")
    long countOverdue(@Param("runningStatuses") Collection<TicketStatus> runningStatuses, @Param("now") Instant now);
}
