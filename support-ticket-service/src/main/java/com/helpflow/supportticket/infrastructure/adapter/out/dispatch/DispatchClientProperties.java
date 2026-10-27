package com.helpflow.supportticket.infrastructure.adapter.out.dispatch;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Where technician-dispatch-service lives and how long we are willing to wait for it.
 */
@Validated
@ConfigurationProperties(prefix = "app.dispatch")
public record DispatchClientProperties(@NotBlank String baseUrl, Duration connectTimeout, Duration readTimeout) {

    public DispatchClientProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(1) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(2) : readTimeout;
    }
}
