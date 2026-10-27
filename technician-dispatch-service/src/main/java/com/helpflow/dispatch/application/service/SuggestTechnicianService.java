package com.helpflow.dispatch.application.service;

import com.helpflow.dispatch.application.exception.DispatchApplicationException;
import com.helpflow.dispatch.application.port.in.SuggestTechnicianUseCase;
import com.helpflow.dispatch.domain.exception.DomainRuleViolationException;
import com.helpflow.dispatch.domain.model.TechnicianSuggestion;
import com.helpflow.dispatch.domain.service.DeterministicTechnicianSelector;

public class SuggestTechnicianService implements SuggestTechnicianUseCase {

    private final DeterministicTechnicianSelector selector;

    public SuggestTechnicianService(DeterministicTechnicianSelector selector) {
        this.selector = selector;
    }

    @Override
    public TechnicianSuggestion suggest(Command command) {
        if (command == null) {
            throw new DispatchApplicationException("Command cannot be null");
        }
        try {
            return selector.select(command.ticketId(), command.category(), command.priority());
        } catch (DomainRuleViolationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DispatchApplicationException("Could not calculate technician suggestion: " + ex.getMessage(), ex);
        }
    }
}
