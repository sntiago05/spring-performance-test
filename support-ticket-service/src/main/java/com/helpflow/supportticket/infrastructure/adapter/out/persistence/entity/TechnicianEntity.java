package com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity;

import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "technicians")
@Getter
@Setter
@NoArgsConstructor
public class TechnicianEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String code;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category specialty;

    @Column(nullable = false)
    private String site;

    @Column(name = "max_active_tickets", nullable = false)
    private int maxActiveTickets;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
}
