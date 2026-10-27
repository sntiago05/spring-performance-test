package com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity;

import com.helpflow.supportticket.domain.model.HistoryAction;
import com.helpflow.supportticket.domain.model.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket_history")
@Getter
@Setter
@NoArgsConstructor
public class TicketHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private TicketEntity ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HistoryAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private TicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private TicketStatus toStatus;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(length = 1000)
    private String observations;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
