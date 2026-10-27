package com.helpflow.supportticket.application.model;

/**
 * Framework-neutral pagination request. {@code sortBy} is a property name already validated by the
 * input adapter against a whitelist.
 */
public record PageQuery(int page, int size, String sortBy, boolean descending) {

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size, null, false);
    }
}
