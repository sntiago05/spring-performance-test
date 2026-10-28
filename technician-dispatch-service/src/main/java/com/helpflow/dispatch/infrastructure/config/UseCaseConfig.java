package com.helpflow.dispatch.infrastructure.config;

import com.helpflow.dispatch.application.port.in.SuggestTechnicianUseCase;
import com.helpflow.dispatch.application.service.SuggestTechnicianService;
import com.helpflow.dispatch.domain.model.DispatchTechnician;
import com.helpflow.dispatch.domain.service.DeterministicTechnicianSelector;
import com.helpflow.dispatch.infrastructure.exception.DispatchInfrastructureException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free domain and application classes into the Spring context.
 */
@Configuration
class UseCaseConfig {

    @Bean
    DeterministicTechnicianSelector deterministicTechnicianSelector(DispatchProperties properties) {
        try {
            return new DeterministicTechnicianSelector(properties.technicians().stream()
                    .map(t -> new DispatchTechnician(t.code(), t.specialty()))
                    .toList());
        } catch (Exception ex) {
            throw new DispatchInfrastructureException("Failed to initialize DeterministicTechnicianSelector: " + ex.getMessage(), ex);
        }
    }

    @Bean
    SuggestTechnicianUseCase suggestTechnicianUseCase(DeterministicTechnicianSelector selector) {
        return new SuggestTechnicianService(selector);
    }
}
