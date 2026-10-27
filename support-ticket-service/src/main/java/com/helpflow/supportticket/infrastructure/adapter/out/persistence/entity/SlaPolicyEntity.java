package com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity;

import com.helpflow.supportticket.domain.model.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sla_policies")
@Getter
@Setter
@NoArgsConstructor
public class SlaPolicyEntity {

    @Id
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "resolution_hours", nullable = false)
    private int resolutionHours;

    @Column(name = "escalation_threshold_percent", nullable = false)
    private int escalationThresholdPercent;
}
