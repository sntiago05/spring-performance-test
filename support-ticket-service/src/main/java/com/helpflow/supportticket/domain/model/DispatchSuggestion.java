package com.helpflow.supportticket.domain.model;

/**
 * Answer of the external technician-dispatch service.
 */
public record DispatchSuggestion(String technicianCode, Availability availability, int estimatedHours) {
}
