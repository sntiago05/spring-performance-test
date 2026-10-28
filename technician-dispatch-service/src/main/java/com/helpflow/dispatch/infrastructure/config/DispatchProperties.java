package com.helpflow.dispatch.infrastructure.config;

import com.helpflow.dispatch.domain.model.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The simulated technician pool: codes and specialties. The codes must match the technicians the
 * ticket service knows (seeded TEC-001..TEC-015) for a suggestion to be assignable there.
 */
@Validated
@ConfigurationProperties(prefix = "dispatch")
public record DispatchProperties(@NotEmpty List<@Valid Technician> technicians) {

    public record Technician(@NotBlank String code, @NotNull Category specialty) {
    }
}
