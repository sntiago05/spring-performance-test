package com.helpflow.supportticket.domain.model;

public record Technician(
        Long id,
        String code,
        Long userId,
        String fullName,
        Category specialty,
        String site,
        int maxActiveTickets,
        UserStatus status) {

    public Technician {
        if (maxActiveTickets < 1) {
            throw new IllegalArgumentException("maxActiveTickets must be at least 1");
        }
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVO;
    }

    /**
     * A technician attends the category of their specialty; {@code OTRO} specialists are generalists
     * and any technician can take tickets of the {@code OTRO} category.
     */
    public boolean canHandle(Category category) {
        return specialty == category || specialty == Category.OTRO || category == Category.OTRO;
    }

    /**
     * Regular capacity is {@code maxActiveTickets}; CRITICA tickets may use one overflow slot so that
     * a critical incident is never left unassigned only because the team is full.
     */
    public boolean hasCapacityFor(Priority priority, int activeTickets) {
        int limit = priority == Priority.CRITICA ? maxActiveTickets + 1 : maxActiveTickets;
        return activeTickets < limit;
    }

    public boolean isOnSite(String ticketSite) {
        return site != null && ticketSite != null && site.trim().equalsIgnoreCase(ticketSite.trim());
    }

    public Technician withUser(Long newUserId) {
        return new Technician(id, code, newUserId, fullName, specialty, site, maxActiveTickets, status);
    }

    public Technician withProfile(String fullName, Category specialty, String site, int maxActiveTickets,
            UserStatus status) {
        return new Technician(id, code, userId, fullName, specialty, site, maxActiveTickets, status);
    }
}
