package com.helpflow.supportticket.infrastructure.adapter.out.persistence.repository;

import com.helpflow.supportticket.infrastructure.adapter.out.persistence.entity.TechnicianEntity;

/**
 * Projection: a technician and how many tickets are in their workload.
 */
public record TechnicianLoad(TechnicianEntity technician, long activeTickets) {
}
