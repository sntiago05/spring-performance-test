package com.helpflow.dispatch.infrastructure.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "dispatch")
public record DispatchProperties(@Min(1) int technicianPoolSize) {
}
