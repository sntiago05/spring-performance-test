package com.helpflow.supportticket.infrastructure.adapter.out.persistence;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Translation between the framework-neutral paging model of the application and Spring Data.
 */
final class JpaPaging {

    private JpaPaging() {
    }

    static Pageable toPageable(PageQuery query, String defaultSortProperty) {
        String property = query.sortBy() == null ? defaultSortProperty : query.sortBy();
        Sort.Direction direction = query.descending() ? Sort.Direction.DESC : Sort.Direction.ASC;
        // "id" as tie-breaker keeps pagination stable when the sort column has duplicates.
        Sort sort = "id".equals(property) ? Sort.by(direction, "id") : Sort.by(direction, property).and(Sort.by("id"));
        return PageRequest.of(query.page(), query.size(), sort);
    }

    static <T> PageResult<T> toResult(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
