package com.helpflow.supportticket.infrastructure.adapter.in.web;

import com.helpflow.supportticket.application.model.PageQuery;
import java.util.Set;

/**
 * Builds a {@link PageQuery} from the public {@code page}, {@code size} and {@code sort=field,dir}
 * parameters, accepting only whitelisted sort fields.
 */
final class PageParams {

    private PageParams() {
    }

    static PageQuery of(int page, int size, String sort, Set<String> allowedFields, String defaultField,
            boolean defaultDescending) {
        if (sort == null || sort.isBlank()) {
            return new PageQuery(page, size, defaultField, defaultDescending);
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        if (!allowedFields.contains(field)) {
            throw new InvalidParameterException("sort");
        }
        boolean descending = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());
        if (parts.length > 1 && !descending && !"asc".equalsIgnoreCase(parts[1].trim())) {
            throw new InvalidParameterException("sort");
        }
        return new PageQuery(page, size, field, descending);
    }
}
