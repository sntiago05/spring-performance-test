package com.helpflow.dispatch.infrastructure.config;

import com.helpflow.dispatch.application.port.in.SuggestTechnicianUseCase;
import com.helpflow.dispatch.application.service.SuggestTechnicianService;
import com.helpflow.dispatch.domain.service.DeterministicTechnicianSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free domain and application classes into the Spring context.
 */
@Configuration
class UseCaseConfig {

    @Bean
    DeterministicTechnicianSelector deterministicTechnicianSelector(DispatchProperties properties) {
        return new DeterministicTechnicianSelector(properties.technicianPoolSize());
    }

    @Bean
    SuggestTechnicianUseCase suggestTechnicianUseCase(DeterministicTechnicianSelector selector) {
        return new SuggestTechnicianService(selector);
    }
}
