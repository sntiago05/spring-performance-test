package com.helpflow.dispatch.domain.model;

/**
 * A technician of the simulated pool: only what the simulation needs to suggest a sensible one.
 */
public record DispatchTechnician(String code, Category specialty) {

    /**
     * Same compatibility rule the ticket service applies when assigning: a technician attends their own
     * specialty, generalists ({@code OTRO}) attend everything, and tickets of category {@code OTRO}
     * can be taken by anybody.
     */
    public boolean canHandle(Category category) {
        return specialty == category || specialty == Category.OTRO || category == Category.OTRO;
    }
}
