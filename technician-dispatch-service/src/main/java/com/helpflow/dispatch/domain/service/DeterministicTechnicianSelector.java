package com.helpflow.dispatch.domain.service;

import com.helpflow.dispatch.domain.exception.DomainRuleViolationException;
import com.helpflow.dispatch.domain.model.Availability;
import com.helpflow.dispatch.domain.model.Category;
import com.helpflow.dispatch.domain.model.Priority;
import com.helpflow.dispatch.domain.model.TechnicianSuggestion;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Simulates technician availability. The same ticketId + category + priority always yields the
 * same technician and estimate: the three values are folded into a numeric seed (CRC32, which is
 * stable across JVMs, unlike identity hashes) and the seed is used to pick both results.
 */
public final class DeterministicTechnicianSelector {

    static final int MAX_ESTIMATED_HOURS = 12;

    private final int poolSize;

    public DeterministicTechnicianSelector(int poolSize) {
        if (poolSize < 1) {
            throw new DomainRuleViolationException("Technician pool size must be at least 1");
        }
        this.poolSize = poolSize;
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
        long seed = seedOf(ticketId, category, priority);
        int technicianNumber = (int) (seed % poolSize) + 1;
        int estimatedHours = (int) ((seed / poolSize) % (MAX_ESTIMATED_HOURS + 1));
        return new TechnicianSuggestion(
                ticketId,
                "TEC-%03d".formatted(technicianNumber),
                Availability.fromEstimatedHours(estimatedHours),
                estimatedHours);
    }

    static long seedOf(long ticketId, Category category, Priority priority) {
        CRC32 crc = new CRC32();
        crc.update((ticketId + "|" + category + "|" + priority).getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
}
