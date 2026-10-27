package com.helpflow.dispatch.application.port.in;

import com.helpflow.dispatch.domain.model.Category;
import com.helpflow.dispatch.domain.model.Priority;
import com.helpflow.dispatch.domain.model.TechnicianSuggestion;

public interface SuggestTechnicianUseCase {

    TechnicianSuggestion suggest(Command command);

    record Command(long ticketId, Category category, Priority priority) {
    }
}
