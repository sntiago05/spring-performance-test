package com.helpflow.dispatch.application.service;

import com.helpflow.dispatch.application.port.in.SuggestTechnicianUseCase;
import com.helpflow.dispatch.domain.model.TechnicianSuggestion;
import com.helpflow.dispatch.domain.service.DeterministicTechnicianSelector;

public class SuggestTechnicianService implements SuggestTechnicianUseCase {

    private final DeterministicTechnicianSelector selector;

    public SuggestTechnicianService(DeterministicTechnicianSelector selector) {
        this.selector = selector;
    }

    @Override
    public TechnicianSuggestion suggest(Command command) {
        return selector.select(command.ticketId(), command.category(), command.priority());
    }
}
