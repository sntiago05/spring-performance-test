package com.helpflow.dispatch.domain.service;

import com.helpflow.dispatch.domain.exception.DomainRuleViolationException;
import com.helpflow.dispatch.domain.model.Availability;
import com.helpflow.dispatch.domain.model.Category;
import com.helpflow.dispatch.domain.model.DispatchTechnician;
import com.helpflow.dispatch.domain.model.Priority;
import com.helpflow.dispatch.domain.model.TechnicianSuggestion;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Simulates technician availability. The same ticketId + category + priority always yields the
 * same technician and estimate: the three values are folded into a numeric seed (CRC32, which is
 * stable across JVMs, unlike identity hashes) and the seed is used to pick both results.
 *
 * <p>The technician is picked among the pool members that can handle the ticket category, so the
 * suggestion is compatible by construction; the ticket service still validates capacity, status and
 * SLA before accepting it.
 */
public final class DeterministicTechnicianSelector {

    static final int MAX_ESTIMATED_HOURS = 12;

    private final List<DispatchTechnician> pool;

    public DeterministicTechnicianSelector(List<DispatchTechnician> technicians) {
        if (technicians == null || technicians.isEmpty()) {
            throw new DomainRuleViolationException("The technician pool must have at least one technician");
        }
        Set<String> codes = new HashSet<>();
        for (DispatchTechnician technician : technicians) {
            if (technician == null || technician.code() == null || technician.specialty() == null) {
                throw new DomainRuleViolationException("Every technician needs a code and a specialty");
            }
            if (!codes.add(technician.code())) {
                throw new DomainRuleViolationException("Duplicated technician code: " + technician.code());
            }
        }
        // Sorted by code: the pick must not depend on the order in which the pool was configured.
        this.pool = technicians.stream().sorted(Comparator.comparing(DispatchTechnician::code)).toList();
    }

    public TechnicianSuggestion select(long ticketId, Category category, Priority priority) {
        if (ticketId <= 0) {
            throw new DomainRuleViolationException("ticketId must be positive");
        }
        if (category == null) {
            throw new DomainRuleViolationException("category must not be null");
        }
        if (priority == null) {
            throw new DomainRuleViolationException("prioridad must not be null");
        }
        List<DispatchTechnician> candidates = pool.stream().filter(t -> t.canHandle(category)).toList();
        if (candidates.isEmpty()) {
            throw new DomainRuleViolationException("No technician of the pool can handle the category " + category);
        }
        long seed = seedOf(ticketId, category, priority);
        DispatchTechnician technician = candidates.get((int) (seed % candidates.size()));
        int estimatedHours = (int) ((seed / candidates.size()) % (MAX_ESTIMATED_HOURS + 1));
        return new TechnicianSuggestion(
                ticketId,
                technician.code(),
                Availability.fromEstimatedHours(estimatedHours),
                estimatedHours);
    }

    static long seedOf(long ticketId, Category category, Priority priority) {
        CRC32 crc = new CRC32();
        crc.update((ticketId + "|" + category + "|" + priority).getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
}
