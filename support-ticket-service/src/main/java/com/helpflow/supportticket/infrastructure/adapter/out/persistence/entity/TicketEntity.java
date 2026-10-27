package com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity;

import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false, updatable = false)
    private UserEntity requester;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(nullable = false)
    private String site;

    @Column(name = "affected_asset", nullable = false)
    private String affectedAsset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sla_due_at", nullable = false, updatable = false)
    private Instant slaDueAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private TechnicianEntity assignedTechnician;

    @Column(name = "first_response_at")
    private Instant firstResponseAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(nullable = false)
    private boolean escalated;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached;

    @Version
    private Long version;

    /**
     * Inverse side of Ticket 1-N History. Lazy and batch-loaded; the history endpoint reads it through
     * its own ordered query.
     */
    @OneToMany(mappedBy = "ticket")
    @BatchSize(size = 16)
    private List<TicketHistoryEntity> history = new ArrayList<>();
}
