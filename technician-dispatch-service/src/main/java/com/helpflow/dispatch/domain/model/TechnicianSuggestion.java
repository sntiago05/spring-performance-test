package com.helpflow.dispatch.domain.model;

public record TechnicianSuggestion(
        long ticketId,
        String technicianId,
        Availability availability,
        int estimatedHours) {
}
