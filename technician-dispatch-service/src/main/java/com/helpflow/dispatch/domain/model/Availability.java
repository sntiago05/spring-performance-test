package com.helpflow.dispatch.domain.model;

/**
 * Availability bucket derived from the estimated attention time.
 */
public enum Availability {
    ALTA, MEDIA, BAJA;

    private static final int HIGH_MAX_HOURS = 2;
    private static final int MEDIUM_MAX_HOURS = 8;

    public static Availability fromEstimatedHours(int hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Estimated hours cannot be negative: " + hours);
        }
        if (hours <= HIGH_MAX_HOURS) {
            return ALTA;
        }
        return hours <= MEDIUM_MAX_HOURS ? MEDIA : BAJA;
    }
}
