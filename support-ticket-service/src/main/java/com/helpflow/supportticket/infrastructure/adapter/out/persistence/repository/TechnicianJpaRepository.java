package com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TechnicianEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TechnicianJpaRepository
        extends JpaRepository<TechnicianEntity, Long>, JpaSpecificationExecutor<TechnicianEntity> {

    Optional<TechnicianEntity> findByUserId(Long userId);

    List<TechnicianEntity> findByUserIdIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TechnicianEntity t where t.id = :id")
    Optional<TechnicianEntity> findByIdForUpdate(@Param("id") Long id);

    /**
     * One query for the whole team: avoids counting tickets technician by technician.
     */
    @Query("""
            select new com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository.TechnicianLoad(
                t, count(k.id))
            from TechnicianEntity t
            left join TicketEntity k on k.assignedTechnician = t and k.status in :workloadStatuses
            group by t
            """)
    List<TechnicianLoad> findAllWithWorkload(@Param("workloadStatuses") Collection<TicketStatus> workloadStatuses);

    @Query("select count(k) from TicketEntity k where k.assignedTechnician.id = :id and k.status in :statuses")
    int countWorkload(@Param("id") Long technicianId, @Param("statuses") Collection<TicketStatus> statuses);

    @Query(value = "select nextval('technician_code_seq')", nativeQuery = true)
    long nextCodeNumber();
}
