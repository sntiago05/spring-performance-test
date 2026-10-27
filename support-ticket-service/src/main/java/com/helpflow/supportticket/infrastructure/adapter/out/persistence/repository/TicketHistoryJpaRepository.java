package com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TicketHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketHistoryJpaRepository extends JpaRepository<TicketHistoryEntity, Long> {

    List<TicketHistoryEntity> findByTicketIdOrderByOccurredAtAscIdAsc(Long ticketId);
}
